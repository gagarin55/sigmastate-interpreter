package sigmastate.helpers

import org.ergoplatform
import org.ergoplatform.ErgoBox
import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, TokenId}
import org.scalatest.prop.{PropertyChecks, GeneratorDrivenPropertyChecks}
import org.scalatest.{PropSpec, Matchers}
import scorex.crypto.hash.Blake2b256
import sigmastate.Values.{TrueLeaf, Value, GroupElementConstant, EvaluatedValue}
import sigmastate.eval.{RuntimeIRContext, CompiletimeCosting, Evaluation}
import sigmastate.interpreter.CryptoConstants
import sigmastate.interpreter.Interpreter.ScriptEnv
import sigmastate.lang.{TransformingSigmaBuilder, SigmaCompiler}
import sigmastate.{SGroupElement, SBoolean, SType}

import scala.language.implicitConversions
import scalan.{TestUtils, TestContexts}

trait SigmaTestingCommons extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers with TestUtils with TestContexts {


  val fakeSelf: ErgoBox = createBox(0, TrueLeaf)

  //fake message, in a real-life a message is to be derived from a spending transaction
  val fakeMessage = Blake2b256("Hello World")

  implicit def grElemConvert(leafConstant: GroupElementConstant): CryptoConstants.EcPointType = leafConstant.value

  implicit def grLeafConvert(elem: CryptoConstants.EcPointType): Value[SGroupElement.type] = GroupElementConstant(elem)

  val compiler = new SigmaCompiler(TransformingSigmaBuilder)

  def compile(env: ScriptEnv, code: String): Value[SType] = {
    compiler.compile(env, code)
  }

  def createBox(value: Int,
                proposition: Value[SBoolean.type],
                additionalTokens: Seq[(TokenId, Long)] = Seq(),
                additionalRegisters: Map[NonMandatoryRegisterId, _ <: EvaluatedValue[_ <: SType]] = Map())
    = ergoplatform.ErgoBox(value, proposition, additionalTokens, additionalRegisters)

  class ScalanCtx extends TestContext with RuntimeIRContext with CompiletimeCosting
}
