package sigmastate

import java.math.BigInteger

import com.google.common.primitives.Longs
import gf2t.GF2_192_Poly
import scapi.sigma.{SigmaProtocol, SigmaProtocolCommonInput, SigmaProtocolPrivateInput, _}
import scorex.crypto.hash.{Blake2b256, CryptographicHash32, Sha256}
import sigmastate.SCollection.SByteArray
import sigmastate.Values._
import sigmastate.interpreter.{Context, Interpreter}
import sigmastate.serialization.OpCodes
import sigmastate.serialization.OpCodes._
import sigmastate.utxo.CostTable.Cost
import sigmastate.utxo.Transformer
import sigmastate.utils.Helpers._
import sigmastate.utils.Extensions._

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * AND conjunction for sigma propositions
  */
case class CAND(sigmaBooleans: Seq[SigmaBoolean]) extends SigmaBoolean {
  override val opCode: OpCode = OpCodes.Undefined

  override def cost[C <: Context[C]](context: C): Long =
    sigmaBooleans.map(_.cost(context)).sum + sigmaBooleans.length * Cost.AndPerChild + Cost.AndDeclaration
}

/**
  * OR disjunction for sigma propositions
  */
case class COR(sigmaBooleans: Seq[SigmaBoolean]) extends SigmaBoolean {
  override val opCode: OpCode = OpCodes.Undefined

  override def cost[C <: Context[C]](context: C): Long =
    sigmaBooleans.map(_.cost(context)).sum + sigmaBooleans.length * Cost.OrPerChild + Cost.OrDeclaration
}

/**
  * THRESHOLD connector for sigma propositions
  */
case class CTHRESHOLD(k: Int, sigmaBooleans: Seq[SigmaBoolean]) extends SigmaBoolean {
  assert(k >= 0 && k <= 255 && sigmaBooleans.length >= k && sigmaBooleans.length <= 255)
  override val opCode: OpCode = OpCodes.AtLeastCode

  // TODO: what should cost be?
  override def cost[C <: Context[C]](context: C): Long =
    sigmaBooleans.map(_.cost(context)).sum + sigmaBooleans.length * Cost.OrPerChild + Cost.OrDeclaration
}

trait SigmaProofOfKnowledgeTree[SP <: SigmaProtocol[SP], S <: SigmaProtocolPrivateInput[SP, _]]
  extends SigmaBoolean with SigmaProtocolCommonInput[SP]


/**
  * OR logical conjunction
  * todo: reduce AND + OR boilerplate by introducing a Connective superclass for both
  */
case class OR(input: Value[SCollection[SBoolean.type]])
  extends Transformer[SCollection[SBoolean.type], SBoolean.type] with NotReadyValueBoolean {

  override val opCode: OpCode = OrCode

  override def cost[C <: Context[C]](context: C): Long =
    input.cost(context) + Cost.AndDeclaration


  //todo: reduce such boilerplate around AND/OR, folders, map etc
  override def transformationReady: Boolean =
    input.evaluated && input.matchCase(
      _.items.forall(_.evaluated),
      _ => true,
      _.items.forall(_.evaluated)
    )

  override def function(intr: Interpreter, ctx: Context[_], input: EvaluatedValue[SCollection[SBoolean.type]]): Value[SBoolean.type] = {
    @tailrec
    def iterChildren(children: Seq[Value[SBoolean.type]],
                     currentBuffer: mutable.Buffer[Value[SBoolean.type]]): mutable.Buffer[Value[SBoolean.type]] = {
      if (children.isEmpty) currentBuffer else children.head match {
        case TrueLeaf => mutable.Buffer(TrueLeaf)
        case FalseLeaf => iterChildren(children.tail, currentBuffer)
        case s: Value[SBoolean.type] => iterChildren(children.tail, currentBuffer += s)
      }
    }

    input.matchCase(in => {
        val reduced = iterChildren(in.items, mutable.Buffer())
        reduced.size match {
          case 0 => FalseLeaf
          case 1 => reduced.head
          case _ =>
            if (reduced.forall(_.isInstanceOf[SigmaBoolean])) COR(reduced.map(_.asInstanceOf[SigmaBoolean]))
            else OR(reduced)
        }
      },
      c => if (anyOf(c.value)) TrueLeaf else FalseLeaf,
      _ => ???
    )
  }
}


/**
  * OR logical conjunction
  */
object OR {
  def apply(children: Seq[Value[SBoolean.type]]): OR =
    OR(ConcreteCollection(children.toIndexedSeq))

  def apply(head: Value[SBoolean.type], tail: Value[SBoolean.type]*): OR = apply(head +: tail)
}

/**
  * AND logical conjunction
  */
case class AND(input: Value[SCollection[SBoolean.type]])
  extends Transformer[SCollection[SBoolean.type], SBoolean.type]
    with NotReadyValueBoolean {

  override val opCode: OpCode = AndCode

  override def cost[C <: Context[C]](context: C): Long =
    input.cost(context) + Cost.AndDeclaration

  //todo: reduce such boilerplate around AND/OR, folders, map etc
  override def transformationReady: Boolean =
    input.evaluated && input.matchCase(
      _.items.forall(_.evaluated),
      _ => true,
      _.items.forall(_.evaluated)
    )

  override def function(intr: Interpreter, ctx: Context[_], input: EvaluatedValue[SCollection[SBoolean.type]]): Value[SBoolean.type] = {
    @tailrec
    def iterChildren(children: IndexedSeq[Value[SBoolean.type]],
                     currentBuffer: mutable.Buffer[Value[SBoolean.type]]): mutable.Buffer[Value[SBoolean.type]] = {
      if (children.isEmpty) currentBuffer else children.head match {
        case FalseLeaf => mutable.Buffer(FalseLeaf)
        case TrueLeaf => iterChildren(children.tail, currentBuffer)
        case and: CAND => iterChildren(and.sigmaBooleans.toIndexedSeq ++ children.tail, currentBuffer)
        case s: Value[SBoolean.type] => iterChildren(children.tail, currentBuffer += s)
      }
    }

    input.matchCase(in => {
        val reduced = iterChildren(in.items, mutable.Buffer())
        reduced.size match {
          case 0 => TrueLeaf
          case 1 => reduced.head
          case _ =>
            // TODO we may have Sigma and Boolean values in different order
            // current implementation is "all or nothing"
            if (reduced.forall(_.isInstanceOf[SigmaBoolean]))
              CAND(reduced.map(_.asInstanceOf[SigmaBoolean]))
            else if (reduced.forall(!_.isInstanceOf[SigmaBoolean]))
              AND(reduced)
            else
              Interpreter.error(
                s"Conjunction $input was reduced to mixed Sigma and Boolean conjunction which is not supported: $reduced")
      }
      },
      c => if (allOf(c.value)) TrueLeaf else FalseLeaf,
      _ => ???
    )
  }
}

/**
  * AND logical conjunction
  */
object AND {
  def apply(children: Seq[Value[SBoolean.type]]): AND =
    AND(ConcreteCollection(children.toIndexedSeq))

  def apply(head: Value[SBoolean.type], tail: Value[SBoolean.type]*): AND = apply(head +: tail)
}

/** ATLEAST has two inputs: integer bound and children same as in AND/OR
// It should go through the children.
// If child is true, remove child and reduce bound. If child is false, remove child.
// If at any point bound>number of children, the result is false.
// If at any bound <=0, result is true.
// If at any point bound == 1, convert to OR. If at any point bound == number of children, convert to and.
// Once you have only sigma propositions below, output CTHRESHOLD
// Somewhere we should also make sure that number of children doesn't exceed 255
// (this will ensure bound is between 2 and 254, because otherwise one of the conditions above will apply and it will
// be converted to one of true, false, and, or)
*/
case class AtLeast(k: Value[SInt.type], input: Value[SCollection[SBoolean.type]])
    extends Transformer[SCollection[SBoolean.type], SBoolean.type]
        with NotReadyValueBoolean {
  override val opCode: OpCode = AtLeastCode

  override def cost[C <: Context[C]](context: C): Long =
    k.cost(context) + input.cost(context) + Cost.AtLeastDeclaration

  override def transformationReady: Boolean =
    k.evaluated && input.evaluated && input.matchCase(
      _.items.forall(_.evaluated),
      _ => true,
      _.items.forall(_.evaluated)
    )

  private def reduce(k: Int, children: Seq[Value[SBoolean.type]]): Value[SBoolean.type] = {
    val sigmas = mutable.Buffer[SigmaBoolean]()
    var bound = k
    var childrenLeft = children.length
    // we should make sure that number of children doesn't exceed 255
    // (this will ensure bound is between 2 and 254, because otherwise one of the conditions above will apply and it will
    // be converted to one of true, false, and, or)
    assert(childrenLeft <= 255)
    for (iChild <- children.indices) {
      children(iChild) match {
        case TrueLeaf => // If child is true, remove child and reduce bound.
          childrenLeft -= 1
          bound -= 1
        case FalseLeaf => // If child is false, remove child, leave bound unchanged.
          childrenLeft -= 1
        case sigma: SigmaBoolean => sigmas += sigma
      }
      //If at any point bound > number of children, the result is false.
      if (bound > childrenLeft) return FalseLeaf
      // If at any point bound <= 0, result is true.
      if (bound <= 0) return TrueLeaf
      // If at any point bound == 1, convert to OR.
      if (bound == 1)
        return OR(sigmas ++ children.slice(iChild + 1, children.length))
      // If at any point bound == number of children, convert to and.
      if (bound == childrenLeft)
        return AND(sigmas ++ children.slice(iChild + 1, children.length))
      // Once you have only sigma propositions below, output CTHRESHOLD
    }
    CTHRESHOLD(bound, sigmas)
  }

  override def function(intr: Interpreter, ctx: Context[_], input: EvaluatedValue[SCollection[SBoolean.type]]): Value[SBoolean.type] = {
    val k = this.k.asInstanceOf[EvaluatedValue[SInt.type]].value
    input.matchCase(
      cc => {
        reduce(k, cc.items)
      },
      c => if (c.value.filter(_ == true).length == k) TrueLeaf else FalseLeaf,
      _ => ???
    )
  }
}

/**
  * Logical threshold
  */
object AtLeast {
  def apply(children: Seq[Value[SBoolean.type]]): AND =
    AND(ConcreteCollection(children.toIndexedSeq))

  def apply(head: Value[SBoolean.type], tail: Value[SBoolean.type]*): AND = apply(head +: tail)
}

/**
  * Up cast for Numeric types
  */
case class Upcast[T <: SNumericType, R <: SNumericType](input: Value[T], tpe: R)
  extends Transformer[T, R] {
  require(input.tpe.isInstanceOf[SNumericType], s"Cannot create Upcast node for non-numeric type ${input.tpe}")
  override val opCode: OpCode = OpCodes.UpcastCode

  override def function(intr: Interpreter, ctx: Context[_], input: EvaluatedValue[T]): Value[R] =
    Constant(this.tpe.upcast(input.value.asInstanceOf[AnyVal]), this.tpe)

  override def cost[C <: Context[C]](context: C): Long = input.cost(context) + 1
}

/**
  * Down cast for Numeric types
  */
case class Downcast[T <: SNumericType, R <: SNumericType](input: Value[T], tpe: R)
  extends Transformer[T, R] {
  require(input.tpe.isInstanceOf[SNumericType], s"Cannot create Downcast node for non-numeric type ${input.tpe}")
  override val opCode: OpCode = OpCodes.DowncastCode

  override def function(intr: Interpreter, ctx: Context[_], input: EvaluatedValue[T]): Value[R] =
    Constant(this.tpe.downcast(input.value.asInstanceOf[AnyVal]), this.tpe)

  override def cost[C <: Context[C]](context: C): Long = input.cost(context) + 1
}

/**
  * Cast SLong to SByteArray
  */
case class LongToByteArray(input: Value[SLong.type])
    extends Transformer[SLong.type, SByteArray] with NotReadyValueByteArray {
  override val opCode: OpCode = OpCodes.LongToByteArrayCode

  override def function(intr: Interpreter, ctx: Context[_], bal: EvaluatedValue[SLong.type]): Value[SByteArray] =
    ByteArrayConstant(Longs.toByteArray(bal.value))

  override def cost[C <: Context[C]](context: C): Long = input.cost(context) + 1 //todo: externalize cost
}

/**
  * Cast SByteArray to SBigInt
  */
case class ByteArrayToBigInt(input: Value[SByteArray])
  extends Transformer[SByteArray, SBigInt.type] with NotReadyValueBigInt {

  override val opCode: OpCode = OpCodes.ByteArrayToBigIntCode

  override def function(intr: Interpreter, ctx: Context[_], bal: EvaluatedValue[SByteArray]): Value[SBigInt.type] =
    BigIntConstant(new BigInteger(1, bal.value))

  override def cost[C <: Context[C]](context: C): Long = input.cost(context) + 1 //todo: externalize cost
}

trait CalcHash extends Transformer[SByteArray, SByteArray] with NotReadyValueByteArray {
  val input: Value[SByteArray]

  val hashFn: CryptographicHash32

  override def function(intr: Interpreter, ctx: Context[_], bal: EvaluatedValue[SByteArray]): Value[SByteArray] =
    ByteArrayConstant(hashFn.apply(bal.value))

  override def cost[C <: Context[C]](context: C): Long = input.cost(context) + Cost.Blake256bDeclaration
}

/**
  * Calculate Blake2b hash from `input`
  */
case class CalcBlake2b256(override val input: Value[SByteArray]) extends CalcHash {
  override val opCode: OpCode = OpCodes.CalcBlake2b256Code

  override val hashFn: CryptographicHash32 = Blake2b256
}

/**
  * Calculate Sha256 hash from `input`
  */
case class CalcSha256(override val input: Value[SByteArray]) extends CalcHash {
  override val opCode: OpCode = OpCodes.CalcSha256Code

  override val hashFn: CryptographicHash32 = Sha256
}

/**
  * A tree node with left and right descendants
  */
sealed trait Triple[LIV <: SType, RIV <: SType, OV <: SType] extends NotReadyValue[OV] {

  val left : Value[LIV]
  val right: Value[RIV]

  override def cost[C <: Context[C]](context: C): Long =
    left.cost(context) + right.cost(context) + Cost.TripleDeclaration
}

// TwoArgumentsOperation
sealed trait TwoArgumentsOperation[LIV <: SType, RIV <: SType, OV <: SType]
  extends Triple[LIV, RIV, OV]

case class ArithOp[T <: SType](left: Value[T], right: Value[T], opCode: OpCode)
  extends TwoArgumentsOperation[T, T, T] with NotReadyValue[T] {
  override def tpe: T = left.tpe
}


/**
  * XOR for two SByteArray
  */
case class Xor(override val left: Value[SByteArray],
               override val right: Value[SByteArray])
  extends TwoArgumentsOperation[SByteArray, SByteArray, SByteArray]
    with NotReadyValueByteArray {

  override val opCode: OpCode = XorCode
}

case class Exponentiate(override val left: Value[SGroupElement.type],
                        override val right: Value[SBigInt.type])
  extends TwoArgumentsOperation[SGroupElement.type, SBigInt.type, SGroupElement.type]
    with NotReadyValueGroupElement {

  override val opCode: OpCode = ExponentiateCode

  override def cost[C <: Context[C]](context: C) = Cost.Exponentiate + left.cost(context) + right.cost(context)
}

case class MultiplyGroup(override val left: Value[SGroupElement.type],
                         override val right: Value[SGroupElement.type])
  extends TwoArgumentsOperation[SGroupElement.type, SGroupElement.type, SGroupElement.type]
    with NotReadyValueGroupElement {

  override val opCode: OpCode = MultiplyGroupCode

  override def cost[C <: Context[C]](context: C) = Cost.MultiplyGroup + left.cost(context) + right.cost(context)
}

case class StringConcat(left: Value[SString.type], right: Value[SString.type])
  extends TwoArgumentsOperation[SString.type, SString.type, SString.type] with NotReadyValue[SString.type] {
  override def tpe: SString.type = left.tpe
  override val opCode: OpCode = StringConcatCode
}

// Relation

sealed trait Relation[LIV <: SType, RIV <: SType] extends Triple[LIV, RIV, SBoolean.type]
  with NotReadyValueBoolean


/**
  * Less operation for SInt
  */
case class LT[T <: SType](override val left: Value[T], override val right: Value[T]) extends Relation[T, T] {
  override val opCode: OpCode = LtCode
}

/**
  * Less or equals operation for SInt
  */
case class LE[T <: SType](override val left: Value[T], override val right: Value[T]) extends Relation[T, T] {
  override val opCode: OpCode = LeCode
}

/**
  * Greater operation for SInt
  */
case class GT[T <: SType](override val left: Value[T], override val right: Value[T]) extends Relation[T, T] {
  override val opCode: OpCode = GtCode
}

/**
  * Greater or equals operation for SInt
  */
case class GE[T <: SType](override val left: Value[T], override val right: Value[T]) extends Relation[T, T] {
  override val opCode: OpCode = GeCode
}

/**
  * Equals operation for SType
  * todo: make EQ to really accept only values of the same type, now EQ(TrueLeaf, IntConstant(5)) is valid
  */
case class EQ[S <: SType](override val left: Value[S], override val right: Value[S])
  extends Relation[S, S] {
  override val opCode: OpCode = EqCode
}

/**
  * Non-Equals operation for SType
  */
case class NEQ[S <: SType](override val left: Value[S], override val right: Value[S])
  extends Relation[S, S] {
  override val opCode: OpCode = NeqCode
}


/**
  * A tree node with three descendants
  */
sealed trait Quadruple[IV1 <: SType, IV2 <: SType, IV3 <: SType, OV <: SType] extends NotReadyValue[OV] {

  val first : Value[IV1]
  val second: Value[IV2]
  val third : Value[IV3]

  override def cost[C <: Context[C]](context: C): Long =
    first.cost(context) + second.cost(context) + third.cost(context) + Cost.QuadrupleDeclaration
}

sealed trait Relation3[IV1 <: SType, IV2 <: SType, IV3 <: SType]
  extends Quadruple[IV1, IV2, IV3, SBoolean.type] with NotReadyValueBoolean


/**
  * Predicate which checks whether a key is in a tree, by using a membership proof
  */
case class IsMember(tree: Value[SAvlTree.type],
                    key: Value[SByteArray],
                    proof: Value[SByteArray]) extends Relation3[SAvlTree.type, SByteArray, SByteArray] {
  override val opCode: OpCode = OpCodes.IsMemberCode

  override lazy val first  = tree
  override lazy val second = key
  override lazy val third  = proof
}

/**
  * If conditional function.
  * Non-lazy - evaluate both branches.
  *
  * @param condition - condition to check
  * @param trueBranch - branch that will be used if condition is true
  * @param falseBranch - branch that will be used if condition is false
  */
case class If[T <: SType](condition: Value[SBoolean.type], trueBranch: Value[T], falseBranch: Value[T])
  extends Quadruple[SBoolean.type, T, T, T] {
  override val opCode: OpCode = OpCodes.IfCode

  override def tpe = trueBranch.tpe

  override lazy val first  = condition
  override lazy val second = trueBranch
  override lazy val third  = falseBranch
}
