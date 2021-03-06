package sigmastate.serialization.trees

import sigmastate.Values._
import sigmastate.lang.Terms._
import sigmastate.serialization.ValueSerializer
import sigmastate.utils.{SigmaByteReader, SigmaByteWriter}
import sigmastate.{Quadruple, _}

case class QuadrupleSerializer[S1 <: SType, S2 <: SType, S3 <: SType, S4 <: SType]
(override val opDesc: QuadrupleCompanion,
 cons: (Value[S1], Value[S2], Value[S3]) => Value[S4])
  extends ValueSerializer[Quadruple[S1, S2, S3, S4]] {

  override def serialize(obj: Quadruple[S1, S2, S3, S4], w: SigmaByteWriter): Unit = {
    w.putValue(obj.first, opDesc.argInfos(0))
    w.putValue(obj.second, opDesc.argInfos(1))
    w.putValue(obj.third, opDesc.argInfos(2))
  }

  override def parse(r: SigmaByteReader): Value[S4] = {
    val arg1 = r.getValue().asValue[S1]
    val arg2 = r.getValue().asValue[S2]
    val arg3 = r.getValue().asValue[S3]
    cons(arg1, arg2, arg3)
  }
}
