package sigmastate.utxo

import java.lang.reflect.InvocationTargetException

import org.ergoplatform.ErgoBox.{R6, R4, R8}
import org.ergoplatform.ErgoLikeContext.dummyPubkey
import org.ergoplatform._
import sigmastate.SCollection.SByteArray
import sigmastate.Values._
import sigmastate._
import sigmastate.helpers.{ErgoLikeTestProvingInterpreter, SigmaTestingCommons}
import sigmastate.interpreter.Interpreter._
import sigmastate.lang.Terms._
import special.sigma.InvalidType
import scalan.BaseCtxTests

class BasicOpsSpecification extends SigmaTestingCommons {
  implicit lazy val IR = new TestingIRContext {
    override val okPrintEvaluatedEntries: Boolean = false
  }

  private val reg1 = ErgoBox.nonMandatoryRegisters.head
  private val reg2 = ErgoBox.nonMandatoryRegisters.tail.head
  val intVar1 = 1.toByte
  val intVar2 = 2.toByte
  val byteVar1 = 3.toByte
  val byteVar2 = 4.toByte
  val bigIntVar1 = 5.toByte
  val bigIntVar2 = 6.toByte
  val bigIntVar3 = 7.toByte
  val byteVar3 = 8.toByte
  val booleanVar = 9.toByte
  val propVar1 = 10.toByte
  val propVar2 = 11.toByte
  val lastExtVar = propVar2

  val ext: Seq[(Byte, EvaluatedValue[_ <: SType])] = Seq(
    (intVar1, IntConstant(1)), (intVar2, IntConstant(2)),
    (byteVar1, ByteConstant(1)), (byteVar2, ByteConstant(2)),
    (bigIntVar1, BigIntConstant(BigInt(10).underlying())), (bigIntVar2, BigIntConstant(BigInt(20).underlying())),
    (booleanVar, TrueLeaf))
  val env = Map(
    "intVar1" -> intVar1, "intVar2" -> intVar2,
    "byteVar1" -> byteVar1, "byteVar2" -> byteVar2, "byteVar3" -> byteVar3,
    "bigIntVar1" -> bigIntVar1, "bigIntVar2" -> bigIntVar2, "bigIntVar3" -> bigIntVar3,
    "trueVar" -> booleanVar,
    "proofVar1" -> propVar1,
    "proofVar2" -> propVar2
    )

  def test(name: String, env: ScriptEnv,
           ext: Seq[(Byte, EvaluatedValue[_ <: SType])],
           script: String, propExp: SValue,
      onlyPositive: Boolean = true) = {
    val prover = new ErgoLikeTestProvingInterpreter() {
      override lazy val contextExtenders: Map[Byte, EvaluatedValue[_ <: SType]] = {
        val p1 = dlogSecrets(0).publicImage
        val p2 = dlogSecrets(1).publicImage
        (ext ++ Seq(propVar1 -> SigmaPropConstant(p1), propVar2 -> SigmaPropConstant(p2))).toMap
      }
    }

    val prop = compileWithCosting(env, script).asBoolValue.toSigmaProp
    if (propExp != null)
      prop shouldBe propExp

    val p3 = prover.dlogSecrets(2).publicImage
    val boxToSpend = ErgoBox(10, prop, additionalRegisters = Map(
      reg1 -> SigmaPropConstant(p3),
      reg2 -> IntConstant(1)),
      creationHeight = 5)

    val newBox1 = ErgoBox(10, prop, creationHeight = 0, boxIndex = 0, additionalRegisters = Map(
      reg1 -> IntConstant(1),
      reg2 -> IntConstant(10)))
    val tx = ErgoLikeTransaction(IndexedSeq(), IndexedSeq(newBox1))

    val ctx = ErgoLikeContext(currentHeight = 0,
      lastBlockUtxoRoot = AvlTreeData.dummy, dummyPubkey, boxesToSpend = IndexedSeq(boxToSpend),
      spendingTransaction = tx, self = boxToSpend)

    val pr = prover.prove(env + (ScriptNameProp -> s"${name}_prove"), prop, ctx, fakeMessage).fold(t => throw t, identity)

    val ctxExt = ctx.withExtension(pr.extension)

    val verifier = new ErgoLikeTestInterpreter
    if (!onlyPositive)
      verifier.verify(env + (ScriptNameProp -> s"${name}_verify"), prop, ctx, pr.proof, fakeMessage).map(_._1).getOrElse(false) shouldBe false //context w/out extensions
    verifier.verify(env + (ScriptNameProp -> s"${name}_verify_ext"), prop, ctxExt, pr.proof, fakeMessage).get._1 shouldBe true
  }

  property("Relation operations") {
    test("R1", env, ext,
      "{ allOf(Coll(getVar[Boolean](trueVar).get, true, true)) }",
      AND(GetVarBoolean(booleanVar).get, TrueLeaf, TrueLeaf).toSigmaProp
    )
    test("R2", env, ext,
      "{ anyOf(Coll(getVar[Boolean](trueVar).get, true, false)) }",
      OR(GetVarBoolean(booleanVar).get, TrueLeaf, FalseLeaf).toSigmaProp
    )
    test("R3", env, ext,
      "{ getVar[Int](intVar2).get > getVar[Int](intVar1).get && getVar[Int](intVar1).get < getVar[Int](intVar2).get }",
      BlockValue(
        Vector(ValDef(1, GetVarInt(2).get), ValDef(2, GetVarInt(1).get)),
        BinAnd(GT(ValUse(1, SInt), ValUse(2, SInt)), LT(ValUse(2, SInt), ValUse(1, SInt)))).asBoolValue.toSigmaProp
    )
    test("R4", env, ext,
      "{ getVar[Int](intVar2).get >= getVar[Int](intVar1).get && getVar[Int](intVar1).get <= getVar[Int](intVar2).get }",
      BlockValue(
        Vector(ValDef(1, GetVarInt(2).get), ValDef(2, GetVarInt(1).get)),
        BinAnd(GE(ValUse(1, SInt), ValUse(2, SInt)), LE(ValUse(2, SInt), ValUse(1, SInt)))).asBoolValue.toSigmaProp
    )
    test("R5", env, ext,
      "{ getVar[Byte](byteVar2).get > getVar[Byte](byteVar1).get && getVar[Byte](byteVar1).get < getVar[Byte](byteVar2).get }",
      BlockValue(
        Vector(ValDef(1, GetVarByte(4).get), ValDef(2, GetVarByte(3).get)),
        BinAnd(GT(ValUse(1, SByte), ValUse(2, SByte)), LT(ValUse(2, SByte), ValUse(1, SByte)))).asBoolValue.toSigmaProp
    )
    test("R6", env, ext,
      "{ getVar[Byte](byteVar2).get >= getVar[Byte](byteVar1).get && getVar[Byte](byteVar1).get <= getVar[Byte](byteVar2).get }",
      BlockValue(
        Vector(ValDef(1, GetVarByte(4).get), ValDef(2, List(), GetVarByte(3).get)),
        BinAnd(GE(ValUse(1, SByte), ValUse(2, SByte)), LE(ValUse(2, SByte), ValUse(1, SByte)))).asBoolValue.toSigmaProp
    )
    test("R7", env, ext,
      "{ getVar[BigInt](bigIntVar2).get > getVar[BigInt](bigIntVar1).get && getVar[BigInt](bigIntVar1).get < getVar[BigInt](bigIntVar2).get }",
      BlockValue(
        Vector(ValDef(1, GetVarBigInt(6).get), ValDef(2, List(), GetVarBigInt(5).get)),
        BinAnd(GT(ValUse(1, SBigInt), ValUse(2, SBigInt)), LT(ValUse(2, SBigInt), ValUse(1, SBigInt)))).asBoolValue.toSigmaProp
    )
    test("R8", env, ext,
      "{ getVar[BigInt](bigIntVar2).get >= getVar[BigInt](bigIntVar1).get && getVar[BigInt](bigIntVar1).get <= getVar[BigInt](bigIntVar2).get }",
      BlockValue(
        Vector(ValDef(1, GetVarBigInt(6).get), ValDef(2, List(), GetVarBigInt(5).get)),
        BinAnd(GE(ValUse(1, SBigInt), ValUse(2, SBigInt)), LE(ValUse(2, SBigInt), ValUse(1, SBigInt)))).asBoolValue.toSigmaProp
    )
  }

  property("SigmaProp operations") {
    test("Prop1", env, ext,
      "{ getVar[SigmaProp](proofVar1).get.isProven }",
      GetVarSigmaProp(propVar1).get
    )
    test("Prop2", env, ext,
      "{ getVar[SigmaProp](proofVar1).get || getVar[SigmaProp](proofVar2).get }",
      SigmaOr(Seq(GetVarSigmaProp(propVar1).get, GetVarSigmaProp(propVar2).get))
    )
    test("Prop3", env, ext,
      "{ getVar[SigmaProp](proofVar1).get && getVar[SigmaProp](proofVar2).get }",
      SigmaAnd(Seq(GetVarSigmaProp(propVar1).get, GetVarSigmaProp(propVar2).get))
    )
    test("Prop4", env, ext,
      "{ getVar[SigmaProp](proofVar1).get.isProven && getVar[SigmaProp](proofVar2).get }",
      SigmaAnd(Seq(GetVarSigmaProp(propVar1).get, GetVarSigmaProp(propVar2).get))
    )
    test("Prop5", env, ext,
      "{ getVar[SigmaProp](proofVar1).get && getVar[Int](intVar1).get == 1 }",
      SigmaAnd(Seq(GetVarSigmaProp(propVar1).get, BoolToSigmaProp(EQ(GetVarInt(intVar1).get, 1))))
    )
    test("Prop6", env, ext,
      "{ getVar[Int](intVar1).get == 1 || getVar[SigmaProp](proofVar1).get }",
      SigmaOr(Seq(BoolToSigmaProp(EQ(GetVarInt(intVar1).get, 1)), GetVarSigmaProp(propVar1).get))
    )
    test("Prop7", env, ext,
      "{ SELF.R4[SigmaProp].get.isProven }",
      ExtractRegisterAs[SSigmaProp.type](Self, reg1).get,
      true
    )
    test("Prop8", env, ext,
      "{ SELF.R4[SigmaProp].get && getVar[SigmaProp](proofVar1).get}",
      SigmaAnd(Seq(ExtractRegisterAs[SSigmaProp.type](Self, reg1).get, GetVarSigmaProp(propVar1).get)),
      true
    )
    test("Prop9", env, ext,
      "{ allOf(Coll(SELF.R4[SigmaProp].get, getVar[SigmaProp](proofVar1).get))}",
      SigmaAnd(Seq(ExtractRegisterAs[SSigmaProp.type](Self, reg1).get, GetVarSigmaProp(propVar1).get)),
      true
    )
    test("Prop10", env, ext,
      "{ anyOf(Coll(SELF.R4[SigmaProp].get, getVar[SigmaProp](proofVar1).get))}",
      SigmaOr(Seq(ExtractRegisterAs[SSigmaProp.type](Self, reg1).get, GetVarSigmaProp(propVar1).get)),
      true
    )
    test("Prop11", env, ext,
        "{ Coll(SELF.R4[SigmaProp].get, getVar[SigmaProp](proofVar1).get).forall({ (p: SigmaProp) => p.isProven }) }",
      SigmaAnd(Seq(ExtractRegisterAs[SSigmaProp.type](Self, reg1).get , GetVarSigmaProp(propVar1).get)),
        true
        )
    test("Prop12", env, ext,
      "{ Coll(SELF.R4[SigmaProp].get, getVar[SigmaProp](proofVar1).get).exists({ (p: SigmaProp) => p.isProven }) }",
      SigmaOr(Seq(ExtractRegisterAs[SSigmaProp.type](Self, reg1).get, GetVarSigmaProp(propVar1).get)),
      true
    )
    test("Prop13", env, ext,
      "{ SELF.R4[SigmaProp].get.propBytes != getVar[SigmaProp](proofVar1).get.propBytes }",
      NEQ(ExtractRegisterAs[SSigmaProp.type](Self, reg1).get.propBytes, GetVarSigmaProp(propVar1).get.propBytes).toSigmaProp,
      true
    )
  }

  property("Arith operations") {
    test("Arith1", env, ext,
      "{ getVar[Int](intVar2).get * 2 + getVar[Int](intVar1).get == 5 }",
      EQ(Plus(Multiply(GetVarInt(intVar2).get, IntConstant(2)), GetVarInt(intVar1).get), IntConstant(5)).toSigmaProp
    )
    test("Arith2", env, ext :+ (bigIntVar3 -> BigIntConstant(50)),
      "{ getVar[BigInt](bigIntVar2).get * 2 + getVar[BigInt](bigIntVar1).get == getVar[BigInt](bigIntVar3).get }",
      EQ(
        Plus(Multiply(GetVarBigInt(bigIntVar2).get, BigIntConstant(2)),
          GetVarBigInt(bigIntVar1).get),
        GetVarBigInt(bigIntVar3).get).toSigmaProp
    )
    test("Arith3", env, ext :+ (byteVar3 -> ByteConstant(5)),
      "{ getVar[Byte](byteVar2).get * 2.toByte + getVar[Byte](byteVar1).get == 5.toByte }",
      EQ(
        Plus(Multiply(GetVarByte(byteVar2).get, ByteConstant(2)),
          GetVarByte(byteVar1).get), ByteConstant(5)).toSigmaProp
    )
    test("Arith4", env, ext,
      "{ getVar[Int](intVar2).get / 2 + getVar[Int](intVar1).get == 2 }",
      EQ(Plus(Divide(GetVarInt(2).get, IntConstant(2)), GetVarInt(1).get),IntConstant(2)).toSigmaProp
    )
    test("Arith5", env, ext,
      "{ getVar[Int](intVar2).get % 2 + getVar[Int](intVar1).get == 1 }",
      EQ(Plus(Modulo(GetVarInt(intVar2).get, IntConstant(2)), GetVarInt(intVar1).get), IntConstant(1)).toSigmaProp
    )
  }

  property("Tuple operations") {
    test("Tup1", env, ext,
      "{ (getVar[Int](intVar1).get, getVar[Int](intVar2).get)._1 == 1 }",
      EQ(GetVarInt(intVar1).get, IntConstant(1)).toSigmaProp
    )
    test("Tup2", env, ext,
      "{ (getVar[Int](intVar1).get, getVar[Int](intVar2).get)._2 == 2 }",
      EQ(GetVarInt(intVar2).get, IntConstant(2)).toSigmaProp
    )
    test("Tup3", env, ext,
      """{ val p = (getVar[Int](intVar1).get, getVar[Int](intVar2).get)
        |  val res = p._1 + p._2
        |  res == 3 }""".stripMargin,
      {
        EQ(Plus(GetVarInt(intVar1).get, GetVarInt(intVar2).get), IntConstant(3)).toSigmaProp
      }
    )
  }

  property("Tuple as Collection operations") {
//    test("TupColl1", env, ext,
//    """{ val p = (getVar[Int](intVar1).get, getVar[Byte](byteVar2).get)
//     |  p.size == 2 }""".stripMargin,
//    {
//      TrueLeaf
//    }, true)
//    test("TupColl2", env, ext,
//    """{ val p = (getVar[Int](intVar1).get, getVar[Byte](byteVar2).get)
//     |  p(0) == 1 }""".stripMargin,
//    {
//      EQ(GetVarInt(intVar1).get, IntConstant(1))
//    })

    val dataVar = (lastExtVar + 1).toByte
    val Colls = IR.sigmaDslBuilderValue.Colls
    val data = Array(Array[Any](Array[Byte](1,2,3), 10L))
    val env1 = env + ("dataVar" -> dataVar)
    val dataType = SCollection(STuple(SCollection(SByte), SLong))
    val ext1 = ext :+ ((dataVar, Constant[SCollection[STuple]](data, dataType)))
//    test("TupColl3", env1, ext1,
//      """{
//        |  val data = getVar[Coll[(Coll[Byte], Long)]](dataVar).get
//        |  data.size == 1
//        |}""".stripMargin,
//      {
//        val data = GetVar(dataVar, dataType).get
//        EQ(SizeOf(data), IntConstant(1))
//      }
//    )
//    test("TupColl4", env1, ext1,
//      """{
//        |  val data = getVar[Coll[(Coll[Byte], Long)]](dataVar).get
//        |  data.exists({ (p: (Coll[Byte], Long)) => p._2 == 10L })
//        |}""".stripMargin,
//      {
//        val data = GetVar(dataVar, dataType).get
//        Exists(data,
//          FuncValue(
//            Vector((1, STuple(SByteArray, SLong))),
//            EQ(SelectField(ValUse(1, STuple(SByteArray, SLong)), 2), LongConstant(10)))
//        )
//      }
//    )
    test("TupColl5", env1, ext1,
      """{
        |  val data = getVar[Coll[(Coll[Byte], Long)]](dataVar).get
        |  data.forall({ (p: (Coll[Byte], Long)) => p._1.size > 0 })
        |}""".stripMargin,
      {
        val data = GetVar(dataVar, dataType).get
        ForAll(data,
          FuncValue(
            Vector((1, STuple(SByteArray, SLong))),
            GT(SizeOf(SelectField(ValUse(1, STuple(SByteArray, SLong)), 1).asCollection[SByte.type]), IntConstant(0)))
        ).toSigmaProp
      }
    )
    test("TupColl6", env1, ext1,
      """{
        |  val data = getVar[Coll[(Coll[Byte], Long)]](dataVar).get
        |  data.map({ (p: (Coll[Byte], Long)) => (p._2, p._1)}).size == 1
        |}""".stripMargin,
      {
        val data = GetVar(dataVar, dataType).get
        EQ(SizeOf(data), IntConstant(1)).toSigmaProp
      }
    )

// TODO uncomment after operations over Any are implemented
//    test(env, ext,
//    """{ val p = (getVar[Int](intVar1).get, getVar[Byte](byteVar2).get)
//     |  p.getOrElse(2, 3).isInstanceOf[Int] }""".stripMargin,
//    {
//      val p = Tuple(GetVarInt(intVar1).get, GetVarByte(byteVar2).get)
//      EQ(ByIndex[SAny.type](p, IntConstant(2), Some(IntConstant(3).asValue[SAny.type])), IntConstant(3))
//    })
  }

  property("GetVar") {
    test("GetVar1", env, ext,
      "{ getVar[Int](intVar2).get == 2 }",
      EQ(GetVarInt(intVar2).get, IntConstant(2)).toSigmaProp
    )
    // wrong type
    assertExceptionThrown(
      test("GetVar2", env, ext,
      "{ getVar[Byte](intVar2).isDefined }",
      GetVarByte(intVar2).isDefined.toSigmaProp,
      true
      ),
      rootCause(_).isInstanceOf[InvalidType])
  }

  property("ExtractRegisterAs") {
//    test("Extract1", env, ext,
//      "{ SELF.R4[SigmaProp].get.isProven }",
//      ExtractRegisterAs[SSigmaProp.type](Self, reg1).get,
//      true
//    )
    // wrong type
    assertExceptionThrown(
      test("Extract2", env, ext,
        "{ SELF.R4[Long].isDefined }",
        ExtractRegisterAs[SLong.type](Self, reg1).isDefined.toSigmaProp,
        true
      ),
      rootCause(_).isInstanceOf[InvalidType])
  }

  property("OptionGet success (SomeValue)") {
    test("Opt1", env, ext,
      "{ getVar[Int](intVar2).get == 2 }",
      EQ(GetVarInt(intVar2).get, IntConstant(2)).toSigmaProp
    )
    test("Opt2", env, ext,
      "{ val v = getVar[Int](intVar2); v.get == 2 }",
      EQ(GetVarInt(intVar2).get, IntConstant(2)).toSigmaProp
    )
  }

  property("OptionGet fail (NoneValue)") {
    assertExceptionThrown(
      test("OptGet1", env, ext,
        "{ getVar[Int](99).get == 2 }",
        EQ(GetVarInt(99).get, IntConstant(2)).toSigmaProp
      ),
      rootCause(_).isInstanceOf[NoSuchElementException])
    assertExceptionThrown(
      test("OptGet2", env, ext,
        "{ SELF.R8[SigmaProp].get.propBytes != getVar[SigmaProp](proofVar1).get.propBytes }",
        NEQ(ExtractRegisterAs[SSigmaProp.type](Self, R8).get.propBytes, GetVarSigmaProp(propVar1).get.propBytes).toSigmaProp,
        true
      ),
      rootCause(_).isInstanceOf[NoSuchElementException])
  }

  property("OptionGetOrElse") {
    test("OptGet1", env, ext,
      "{ SELF.R5[Int].getOrElse(3) == 1 }",
      EQ(ExtractRegisterAs[SInt.type](Self, reg2).getOrElse(IntConstant(3)), IntConstant(1)).toSigmaProp,
      true
    )
    // register should be empty
    test("OptGet2", env, ext,
      "{ SELF.R6[Int].getOrElse(3) == 3 }",
      EQ(ExtractRegisterAs(Self, R6, SOption(SInt)).getOrElse(IntConstant(3)), IntConstant(3)).toSigmaProp,
      true
    )
    test("OptGet3", env, ext,
      "{ getVar[Int](intVar2).getOrElse(3) == 2 }",
      EQ(GetVarInt(intVar2).getOrElse(IntConstant(3)), IntConstant(2)).toSigmaProp,
      true
    )
    // there should be no variable with this id
    test("OptGet4", env, ext,
    "{ getVar[Int](99).getOrElse(3) == 3 }",
      EQ(GetVarInt(99).getOrElse(IntConstant(3)), IntConstant(3)).toSigmaProp,
      true
    )
  }

  property("OptionIsDefined") {
    test("Def1", env, ext,
      "{ SELF.R4[SigmaProp].isDefined }",
      ExtractRegisterAs[SSigmaProp.type](Self, reg1).isDefined.toSigmaProp,
      true
    )
    // no value
    test("Def2", env, ext,
      "{ SELF.R8[Int].isDefined == false }",
      EQ(ExtractRegisterAs[SInt.type](Self, R8).isDefined, FalseLeaf).toSigmaProp,
      true
    )

    test("Def3", env, ext,
      "{ getVar[Int](intVar2).isDefined }",
      GetVarInt(intVar2).isDefined.toSigmaProp,
      true
    )
    // there should be no variable with this id
    test("Def4", env, ext,
      "{ getVar[Int](99).isDefined == false }",
      EQ(GetVarInt(99).isDefined, FalseLeaf).toSigmaProp,
      true
    )
  }

  property("ByteArrayToBigInt: big int should always be positive") {
    test("BATBI1", env, ext,
      "{ byteArrayToBigInt(Coll[Byte](-1.toByte)) > 0 }",
      GT(ByteArrayToBigInt(ConcreteCollection(ByteConstant(-1))), BigIntConstant(0)).toSigmaProp,
      onlyPositive = true
    )
  }

  property("ByteArrayToBigInt: big int should not exceed dlog group order ") {
    val bytes = Array.fill[Byte](500)(1)
    val itemsStr = bytes.map(v => s"$v.toByte").mkString(",")
    assertExceptionThrown(
      test("BATBI1", env, ext,
        s"{ byteArrayToBigInt(Coll[Byte]($itemsStr)) > 0 }",
        GT(ByteArrayToBigInt(ConcreteCollection(bytes.map(ByteConstant(_)))), BigIntConstant(0)).toSigmaProp,
        onlyPositive = true
      ),
      _.getCause.isInstanceOf[RuntimeException]
    )
  }

  property("ExtractCreationInfo") {
    test("Info1", env, ext,
      "SELF.creationInfo._1 == 5",
      EQ(SelectField(ExtractCreationInfo(Self),1),IntConstant(5)).toSigmaProp,
      true
    )
    // suppose to be tx.id + box index
    test("Info2", env, ext,
      "SELF.creationInfo._2.size == 34",
      EQ(SizeOf(SelectField(ExtractCreationInfo(Self),2).asValue[SByteArray]),IntConstant(34)).toSigmaProp,
      true
    )
  }

  property("sigmaProp") {
    test("prop1", env, ext, "sigmaProp(HEIGHT >= 0)",
      BoolToSigmaProp(GE(Height, IntConstant(0))), true)
    test("prop2", env, ext, "sigmaProp(HEIGHT >= 0) && getVar[SigmaProp](proofVar1).get",
      SigmaAnd(Vector(BoolToSigmaProp(GE(Height, IntConstant(0))), GetVarSigmaProp(propVar1).get)), true)
//    println(CostTableStat.costTableString)
  }

//  property("ZKProof") {
//    test("zk1", env, ext, "ZKProof { sigmaProp(HEIGHT >= 0) }",
//      ZKProofBlock(BoolToSigmaProp(GE(Height, LongConstant(0)))), true)
//  }

  property("numeric cast") {
    test("downcast", env, ext,
      "{ getVar[Int](intVar2).get.toByte == 2.toByte }",
      EQ(Downcast(GetVarInt(2).get, SByte), ByteConstant(2)).toSigmaProp,
      onlyPositive = true
    )
    test("upcast", env, ext,
      "{ getVar[Int](intVar2).get.toLong == 2L }",
      EQ(Upcast(GetVarInt(2).get, SLong), LongConstant(2)).toSigmaProp,
      onlyPositive = true
    )
  }

  property("EQ const array vs collection") {
    val byteArrayVar1Value = ByteArrayConstant(Array[Byte](1.toByte, 2.toByte))
    test("EQArrayCollection", env + ("byteArrayVar1" -> byteArrayVar1Value), ext,
      "byteArrayVar1 == Coll[Byte](1.toByte, 2.toByte)",
      EQ(byteArrayVar1Value, ConcreteCollection(Vector(ByteConstant(1), ByteConstant(2)), SByte)).toSigmaProp,
      true
    )
  }

  property("user defined function") {
    test("function", env, ext,
      "{ def inc(i: Int) = i + 1; inc(2) == 3 }",
      EQ(
        Apply(
          FuncValue(Vector((1, SInt)), Plus(ValUse(1, SInt), IntConstant(1))),
          Vector(IntConstant(2))
        ),
        IntConstant(3)).toSigmaProp,
    )
  }

  property("missing variable in env buildValue error") {
    test("missingVar", env, ext,
      """
        |OUTPUTS.forall({(out:Box) =>
        |  out.R5[Int].get >= HEIGHT + 10 &&
        |  blake2b256(out.propositionBytes) != Coll[Byte](1.toByte)
        |})
      """.stripMargin,
      ForAll(Outputs, FuncValue(Vector((1,SBox)),
        BinAnd(
          GE(ExtractRegisterAs(ValUse(1,SBox), ErgoBox.R5, SOption(SInt)).get, Plus(Height, IntConstant(10))),
          NEQ(CalcBlake2b256(ExtractScriptBytes(ValUse(1,SBox))), ConcreteCollection(Vector(ByteConstant(1.toByte)), SByte))
        ))).toSigmaProp,
      true
    )
  }

  property("Nested logical ops 1") {
   test("nestedLogic1", env, ext,
     """{
      |    val c = OUTPUTS(0).R4[Int].get
      |    val d = OUTPUTS(0).R5[Int].get
      |
      |    OUTPUTS.size == 2 &&
      |    OUTPUTS(0).value == SELF.value &&
      |    OUTPUTS(1).value == SELF.value
      |} == false""".stripMargin,
     null,
     true
   )
  }

  ignore("Nested logical ops 2") {
    test("nestedLogic", env, ext,
      """{
       |    val c = OUTPUTS(0).R4[Int].get
       |    val d = OUTPUTS(0).R5[Int].get
       |
       |    OUTPUTS.size == 2 &&
       |    OUTPUTS(0).value == SELF.value &&
       |    OUTPUTS(1).value == SELF.value &&
       |    blake2b256(OUTPUTS(0).propositionBytes) == fullMixScriptHash &&
       |    blake2b256(OUTPUTS(1).propositionBytes) == fullMixScriptHash &&
       |    OUTPUTS(1).R4[GroupElement].get == d &&
       |    OUTPUTS(1).R5[GroupElement].get == c && {
       |      proveDHTuple(g, c, u, d) ||
       |      proveDHTuple(g, d, u, c)
       |    }
       |}""".stripMargin,
      FalseLeaf,
      true
    )
  }
}
