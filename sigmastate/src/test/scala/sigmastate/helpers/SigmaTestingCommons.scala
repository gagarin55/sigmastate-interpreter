package sigmastate.helpers

import org.ergoplatform.ErgoAddressEncoder.TestnetNetworkPrefix
import org.ergoplatform.ErgoBox.NonMandatoryRegisterId
import org.ergoplatform.ErgoScriptPredef.TrueProp
import org.ergoplatform._
import org.ergoplatform.validation.ValidationRules.{CheckCostFunc, CheckCalcFunc}
import org.ergoplatform.validation.ValidationSpecification
import org.scalacheck.Arbitrary.arbByte
import org.scalacheck.Gen
import org.scalatest.prop.{PropertyChecks, GeneratorDrivenPropertyChecks}
import org.scalatest.{PropSpec, Assertion, Matchers}
import scalan.{TestUtils, TestContexts, RType}
import scorex.crypto.hash.{Digest32, Blake2b256}
import sigma.types.IsPrimView
import sigmastate.Values.{Constant, EvaluatedValue, SValue, Value, ErgoTree, GroupElementConstant}
import sigmastate.interpreter.Interpreter.{ScriptNameProp, ScriptEnv}
import sigmastate.interpreter.{CryptoConstants, Interpreter}
import sigmastate.lang.{Terms, TransformingSigmaBuilder, SigmaCompiler}
import sigmastate.serialization.{ValueSerializer, SigmaSerializer}
import sigmastate.{SGroupElement, SType}
import sigmastate.eval.{CompiletimeCosting, IRContext, Evaluation, _}
import sigmastate.interpreter.CryptoConstants.EcPointType
import special.sigma

import scala.annotation.tailrec
import scala.language.implicitConversions

trait SigmaTestingCommons extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers with TestUtils with TestContexts with ValidationSpecification {

  val fakeSelf: ErgoBox = createBox(0, TrueProp)

  val fakeContext: ErgoLikeContext = ErgoLikeContextTesting.dummy(fakeSelf)

  //fake message, in a real-life a message is to be derived from a spending transaction
  val fakeMessage = Blake2b256("Hello World")

  implicit def grElemConvert(leafConstant: GroupElementConstant): EcPointType =
    SigmaDsl.toECPoint(leafConstant.value).asInstanceOf[EcPointType]

  implicit def grLeafConvert(elem: CryptoConstants.EcPointType): Value[SGroupElement.type] = GroupElementConstant(elem)

  val compiler = SigmaCompiler(TestnetNetworkPrefix, TransformingSigmaBuilder)

  def checkSerializationRoundTrip(v: SValue): Unit = {
    val compiledTreeBytes = ValueSerializer.serialize(v)
    withClue(s"(De)Serialization roundtrip failed for the tree:") {
      ValueSerializer.deserialize(compiledTreeBytes) shouldEqual v
    }
  }

  def compileWithoutCosting(env: ScriptEnv, code: String): Value[SType] = compiler.compileWithoutCosting(env, code)

  def compile(env: ScriptEnv, code: String)(implicit IR: IRContext): Value[SType] = {
    val tree = compiler.compile(env, code)
    checkSerializationRoundTrip(tree)
    tree
  }

  def createBox(value: Long,
                proposition: ErgoTree,
                additionalTokens: Seq[(Digest32, Long)] = Seq(),
                additionalRegisters: Map[NonMandatoryRegisterId, _ <: EvaluatedValue[_ <: SType]] = Map())
  = ErgoBox(value, proposition, 0, additionalTokens, additionalRegisters)

  def createBox(value: Long,
                proposition: ErgoTree,
                creationHeight: Int)
  = ErgoBox(value, proposition, creationHeight, Seq(), Map(), ErgoBox.allZerosModifierId)

  /**
    * Create fake transaction with provided outputCandidates, but without inputs and data inputs.
    * Normally, this transaction will be invalid as far as it will break rule that sum of
    * coins in inputs should not be less then sum of coins in outputs, but we're not checking it
    * in our test cases
    */
  def createTransaction(outputCandidates: IndexedSeq[ErgoBoxCandidate]): ErgoLikeTransaction = {
    new ErgoLikeTransaction(IndexedSeq(), IndexedSeq(), outputCandidates)
  }

  def createTransaction(box: ErgoBoxCandidate): ErgoLikeTransaction = createTransaction(IndexedSeq(box))

  def createTransaction(dataInputs: IndexedSeq[ErgoBox],
                        outputCandidates: IndexedSeq[ErgoBoxCandidate]): ErgoLikeTransaction =
    new ErgoLikeTransaction(IndexedSeq(), dataInputs.map(b => DataInput(b.id)), outputCandidates)

  class TestingIRContext extends TestContext with IRContext with CompiletimeCosting {
    override def onCostingResult[T](env: ScriptEnv, tree: SValue, res: RCostingResultEx[T]): Unit = {
      env.get(ScriptNameProp) match {
        case Some(name: String) if saveGraphsInFile =>
          emit(name, Pair(res.calcF, res.costF))
        case _ =>
      }
    }

    override def onEstimatedCost[T](env: ScriptEnv,
                                    tree: SValue,
                                    result: RCostingResultEx[T],
                                    ctx: special.sigma.Context,
                                    estimatedCost: Int): Unit = {
      if (outputEstimatedCost) {
        env.get(Interpreter.ScriptNameProp) match {
          case Some(name: String) =>
            println(s"Cost of $name = $estimatedCost")
          case _ =>
        }
      }
    }

    override private[sigmastate] def onResult[T](env: ScriptEnv,
                                     tree: SValue,
                                     result: RCostingResultEx[T],
                                     ctx: sigma.Context,
                                     estimatedCost: Int,
                                     calcCtx: sigma.Context,
                                     executedResult: sigma.SigmaProp,
                                     executionTime: Long): Unit = {
      if (outputComputedResults) {
        val name = env.get(Interpreter.ScriptNameProp).getOrElse("")
        println(s"ScriptName: $name, EstimatedCost: $estimatedCost, ExecutionTime: $executionTime")
      }
    }
  }

  private def fromPrimView[A](in: A) = {
    in match {
      case IsPrimView(v) => v
      case _ => in
    }
  }

  case class CompiledFunc[A,B]
    (script: String, bindings: Seq[(Byte, EvaluatedValue[_ <: SType])], expr: SValue, func: A => B)
    (implicit val tA: RType[A], val tB: RType[B]) extends Function1[A, B] {
    override def apply(x: A): B = func(x)
  }

  /** The same operations are executed as part of Interpreter.verify() */
  def getCostingResult(env: ScriptEnv, exp: SValue)(implicit IR: IRContext): IR.RCostingResultEx[Any] = {
    val costingRes = IR.doCostingEx(env, exp, true)
    val costF = costingRes.costF
    CheckCostFunc(IR)(IR.asRep[Any => Int](costF))

    val calcF = costingRes.calcF
    CheckCalcFunc(IR)(calcF)
    costingRes
  }

  /** Returns a Scala function which is equivalent to the given function script.
    * The script is embedded into valid ErgoScript which is then compiled to
    * [[sigmastate.Values.Value]] tree.
    * Limitations:
    * 1) DeserializeContext, ConstantPlaceholder is not supported
    * @param funcScript source code of the function
    * @param bindings additional context variables
    */
  def func[A: RType, B: RType]
      (funcScript: String, bindings: (Byte, EvaluatedValue[_ <: SType])*)
      (implicit IR: IRContext): CompiledFunc[A, B] = {
    import IR._
    import IR.Context._;
    val tA = RType[A]
    val tB = RType[B]
    val tpeA = Evaluation.rtypeToSType(tA)
    val tpeB = Evaluation.rtypeToSType(tB)
    val code =
      s"""{
         |  val func = $funcScript
         |  val res = func(getVar[${tA.name}](1).get)
         |  res
         |}
      """.stripMargin
    val env = Interpreter.emptyEnv

    // The following ops are performed by frontend
    // typecheck, create graphs, compile to Tree
    // The resulting tree should be serializable
    val compiledTree = {
      val internalProp = compiler.typecheck(env, code)
      val costingRes = getCostingResult(env, internalProp)
      val calcF = costingRes.calcF
      val tree = IR.buildTree(calcF)
      checkSerializationRoundTrip(tree)
      tree
    }

    // The following is done as part of Interpreter.verify()
    val valueFun = {
      val costingRes = getCostingResult(env, compiledTree)
      val calcF = costingRes.calcF
      val tree = IR.buildTree(calcF)

      // sanity check that buildTree is reverse to buildGraph (see doCostingEx)
      tree shouldBe compiledTree

      val lA = Liftables.asLiftable[SContext, IR.Context](calcF.elem.eDom.liftable)
      val lB = Liftables.asLiftable[Any, Any](calcF.elem.eRange.liftable)
      IR.compile[SContext, Any, IR.Context, Any](IR.getDataEnv, calcF)(lA, lB)
    }

    val f = (in: A) => {
      implicit val cA = tA.classTag
      val x = fromPrimView(in)
      val sigmaCtx = in match {
        case ctx: CostingDataContext =>
          // the context is passed as function argument (this is for testing only)
          // This is to overcome non-functional semantics of context operations
          // (such as Inputs, Height, etc which don't have arguments and refer to the
          // context implicitly).
          // These context operations are introduced by buildTree frontend function
          // (ctx.HEIGHT method call compiled to Height IR node)
          // -------
          // We add ctx as it's own variable with id = 1
          val ctxVar = Extensions.toAnyValue[special.sigma.Context](ctx)(special.sigma.ContextRType)
          val newVars = if (ctx.vars.length < 2) {
            val vars = ctx.vars.toArray
            val buf = new Array[special.sigma.AnyValue](2)
            Array.copy(vars, 0, buf, 0, vars.length)
            buf(1) = ctxVar
            CostingSigmaDslBuilder.Colls.fromArray(buf)
          } else {
            ctx.vars.updated(1, ctxVar)
          }
          ctx.copy(vars = newVars)
        case _ =>
          val ergoCtx = ErgoLikeContextTesting.dummy(createBox(0, TrueProp))
              .withBindings(1.toByte -> Constant[SType](x.asInstanceOf[SType#WrappedType], tpeA))
              .withBindings(bindings: _*)
          ergoCtx.toSigmaContext(IR, isCost = false)
      }
      val (res, _) = valueFun(sigmaCtx)
      res.asInstanceOf[B]
    }
    val Terms.Apply(funcVal, _) = compiledTree.asInstanceOf[SValue]
    CompiledFunc(funcScript, bindings.toSeq, funcVal, f)
  }

  def assertExceptionThrown(fun: => Any, assertion: Throwable => Boolean, clue: => String = ""): Unit = {
    try {
      fun
      fail("exception is expected")
    }
    catch {
      case e: Throwable =>
        if (!assertion(e))
          fail(
            s"""exception check failed on $e (root cause: ${rootCause(e)})
              |clue: $clue
              |trace:
              |${e.getStackTrace.mkString("\n")}}""".stripMargin)
    }
  }

  @tailrec
  final def rootCause(t: Throwable): Throwable =
    if (t.getCause == null) t
    else rootCause(t.getCause)

  protected def roundTripTest[T](v: T)(implicit serializer: SigmaSerializer[T, T]): Assertion = {
    // using default sigma reader/writer
    val bytes = serializer.toBytes(v)
    bytes.nonEmpty shouldBe true
    val r = SigmaSerializer.startReader(bytes)
    val positionLimitBefore = r.positionLimit
    serializer.parse(r) shouldBe v
    r.positionLimit shouldBe positionLimitBefore
  }

  protected def roundTripTestWithPos[T](v: T)(implicit serializer: SigmaSerializer[T, T]): Assertion = {
    val randomBytesCount = Gen.chooseNum(1, 20).sample.get
    val randomBytes = Gen.listOfN(randomBytesCount, arbByte.arbitrary).sample.get.toArray
    val bytes = serializer.toBytes(v)
    serializer.parse(SigmaSerializer.startReader(bytes)) shouldBe v
    serializer.parse(SigmaSerializer.startReader(randomBytes ++ bytes, randomBytesCount)) shouldBe v
  }

}