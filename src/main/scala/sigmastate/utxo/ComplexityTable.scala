package sigmastate.utxo

import org.ergoplatform._
import sigmastate._
import sigmastate.Values._
import sigmastate.lang.Terms._
import sigmastate.serialization.OpCodes.OpCode

object ComplexityTable {

  val MinimalComplexity = 100

  val OpCodeComplexity: Map[OpCode, Int] = Seq(
    Fold.opCode                    -> 4034,  // count = 122
    MapCollection.opCode           -> 2514,  // count = 402
    BinAnd.opCode                  -> 2000,  // count = 21858
    BinOr.opCode                   -> 2000,  // count = 9894
    Exists.opCode                  -> 1997,  // count = 4131
    Apply.opCode                   -> 1592,  // count = 327
    Append.opCode                  -> 1524,  // count = 63
    ForAll.opCode                  -> 1451,  // count = 7952
    XorOf.opCode                   -> 1273,  // count = 2
    GroupGenerator.opCode          -> 1212,  // count = 10
    Filter.opCode                  -> 849,  // count = 1656
    ByteArrayToBigInt.opCode       -> 727,  // count = 9
    LastBlockUtxoRootHash.opCode   -> 726,  // count = 3
    ModQ.opCode                    -> 690,  // count = 1
    GetVar.opCode                  -> 687,  // count = 1150
    Xor.opCode                     -> 632,  // count = 9
    Tuple.opCode                   -> 625,  // count = 26
    SubstConstants.opCode          -> 621,  // count = 131
    CalcSha256.opCode              -> 505,  // count = 6
    OptionGetOrElse.opCode         -> 449,  // count = 108
    ConcreteCollectionBooleanConstant.opCode -> 428,  // count = 3
    CalcBlake2b256.opCode          -> 381,  // count = 609
    FuncValue.opCode               -> 352,  // count = 5687
    OptionIsDefined.opCode         -> 343,  // count = 58
    Negation.opCode                -> 328,  // count = 9
    ByteArrayToLong.opCode         -> 284,  // count = 3
    If.opCode                      -> 284,  // count = 3918
    AtLeast.opCode                 -> 281,  // count = 7540
    ConcreteCollection.opCode      -> 279,  // count = 7956
    BinXor.opCode                  -> 277,  // count = 19
    OR.opCode                      -> 274,  // count = 837
    Inputs.opCode                  -> 274,  // count = 8961
    ModQArithOp.PlusModQ.opCode    -> 272,  // count = 1
    ExtractCreationInfo.opCode     -> 266,  // count = 430
    Exponentiate.opCode            -> 253,  // count = 7
    OptionGet.opCode               -> 238,  // count = 29116
    AND.opCode                     -> 230,  // count = 10153
    EQ.opCode                      -> 227,  // count = 33055
    ArithOp.Min.opCode             -> 227,  // count = 30
    Outputs.opCode                 -> 215,  // count = 29061
    ModQArithOp.MinusModQ.opCode   -> 211,  // count = 1
    LongToByteArray.opCode         -> 209,  // count = 15
    SelectField.opCode             -> 205,  // count = 1217
    ExtractRegisterAs.opCode       -> 197,  // count = 28059
    ArithOp.Modulo.opCode          -> 186,  // count = 34
    ExtractId.opCode               -> 186,  // count = 66
    ArithOp.Max.opCode             -> 185,  // count = 70
    DecodePoint.opCode             -> 184,  // count = 133
    SigmaOr.opCode                 -> 183,  // count = 4666
    SigmaAnd.opCode                -> 177,  // count = 4467
    MultiplyGroup.opCode           -> 176,  // count = 3
    ByIndex.opCode                 -> 174,  // count = 32408
    ExtractBytes.opCode            -> 174,  // count = 17
    Downcast.opCode                -> 168,  // count = 79
    CreateProveDHTuple.opCode      -> 164,  // count = 27
    SizeOf.opCode                  -> 151,  // count = 4952
    Slice.opCode                   -> 143,  // count = 580
    Self.opCode                    -> 117,  // count = 18395
    ExtractBytesWithNoRef.opCode   -> 116,  // count = 1129
    MinerPubkey.opCode             -> 107,  // count = 131
    GT.opCode                      -> 101,  // count = 7137
    ExtractScriptBytes.opCode      -> 100,  // count = 13780
    ArithOp.Plus.opCode            -> 99,  // count = 12850
    LE.opCode                      -> 96,  // count = 3549
    NEQ.opCode                     -> 96,  // count = 2079
    GE.opCode                      -> 95,  // count = 10941
    ArithOp.Minus.opCode           -> 94,  // count = 18200
    ArithOp.Multiply.opCode        -> 94,  // count = 12955
    Upcast.opCode                  -> 94,  // count = 15608
    SigmaPropBytes.opCode          -> 94,  // count = 4135
    ArithOp.Division.opCode        -> 92,  // count = 6809
    LT.opCode                      -> 87,  // count = 8715
    TrueLeaf.opCode                -> 86,  // count = 6764
    BoolToSigmaProp.opCode         -> 84,  // count = 11765
    FalseLeaf.opCode               -> 80,  // count = 4825
    Height.opCode                  -> 80,  // count = 30856
    Constant.opCode                -> 80,  // count = 251669
    SigmaPropIsProven.opCode       -> 78,  // count = 20566
    CreateProveDlog.opCode         -> 76,  // count = 147
    BlockValue.opCode              -> 74,  // count = 895
    ExtractAmount.opCode           -> 74,  // count = 14650
    LogicalNot.opCode              -> 56,  // count = 1420
    Global.opCode                  -> 7,  // count = 3
    ValUse.opCode                  -> 3,  // count = 18771
    Context.opCode                 -> 1,  // count = 72
  ).toMap

  val MethodCallComplexity: Map[(Byte, Byte), Int] = Seq(
    (100.toByte, 13.toByte) -> 3911,  // count = 1, AvlTree.update
    (36.toByte, 7.toByte) -> 2183,  // count = 7, SOption.map
    (7.toByte, 2.toByte) -> 2107,  // count = 2, GroupElement.getEncoded
    (100.toByte, 11.toByte) -> 1960,  // count = 9, AvlTree.getMany
    (12.toByte, 15.toByte) -> 1853,  // count = 820, SCollection.flatMap
    (36.toByte, 8.toByte) -> 1719,  // count = 7, SOption.filter
    (12.toByte, 29.toByte) -> 1588,  // count = 19, SCollection.zip
    (100.toByte, 12.toByte) -> 1460,  // count = 8, AvlTree.insert
    (100.toByte, 4.toByte) -> 1343,  // count = 5, AvlTree.valueLengthOpt
    (100.toByte, 10.toByte) -> 1331,  // count = 22, AvlTree.get
    (100.toByte, 14.toByte) -> 1229,  // count = 5, AvlTree.remove
    (105.toByte, 1.toByte) -> 1214,  // count = 3, PreHeader.version
    (104.toByte, 1.toByte) -> 1157,  // count = 3, Header.id
    (12.toByte, 21.toByte) -> 966,  // count = 5, SCollection.updateMany
    (101.toByte, 1.toByte) -> 900,  // count = 25, Context.dataInputs
    (100.toByte, 9.toByte) -> 809,  // count = 13, AvlTree.contains
    (12.toByte, 26.toByte) -> 788,  // count = 6, SCollection.indexOf
    (100.toByte, 7.toByte) -> 694,  // count = 1, AvlTree.isRemoveAllowed
    (100.toByte, 5.toByte) -> 671,  // count = 1, AvlTree.isInsertAllowed
    (12.toByte, 19.toByte) -> 557,  // count = 5, SCollection.patch
    (101.toByte, 8.toByte) -> 510,  // count = 1, Context.selfBoxIndex
    (100.toByte, 2.toByte) -> 452,  // count = 3, AvlTree.enabledOperations
    (105.toByte, 7.toByte) -> 451,  // count = 3, PreHeader.votes
    (105.toByte, 6.toByte) -> 447,  // count = 3, PreHeader.minerPk
    (7.toByte, 5.toByte) -> 444,  // count = 1, GroupElement.negate
    (104.toByte, 14.toByte) -> 412,  // count = 3, Header.powDistance
    (100.toByte, 1.toByte) -> 384,  // count = 6, AvlTree.digest
    (104.toByte, 13.toByte) -> 378,  // count = 3, Header.powNonce
    (101.toByte, 3.toByte) -> 357,  // count = 17, Context.preHeader
    (104.toByte, 7.toByte) -> 343,  // count = 3, Header.timestamp
    (105.toByte, 3.toByte) -> 339,  // count = 3, PreHeader.timestamp
    (104.toByte, 10.toByte) -> 335,  // count = 3, Header.extensionRoot
    (12.toByte, 20.toByte) -> 329,  // count = 5, SCollection.updated
    (100.toByte, 3.toByte) -> 328,  // count = 5, AvlTree.keyLength
    (105.toByte, 5.toByte) -> 328,  // count = 5, PreHeader.height
    (104.toByte, 9.toByte) -> 326,  // count = 3, Header.height
    (105.toByte, 2.toByte) -> 323,  // count = 3, PreHeader.parentId
    (105.toByte, 4.toByte) -> 319,  // count = 3, PreHeader.nBits
    (104.toByte, 8.toByte) -> 317,  // count = 3, Header.nBits
    (104.toByte, 2.toByte) -> 316,  // count = 3, Header.version
    (101.toByte, 2.toByte) -> 315,  // count = 33, Context.headers
    (104.toByte, 4.toByte) -> 313,  // count = 3, Header.ADProofsRoot
    (104.toByte, 12.toByte) -> 303,  // count = 3, Header.powOnetimePk
    (104.toByte, 11.toByte) -> 303,  // count = 3, Header.minerPk
    (104.toByte, 15.toByte) -> 302,  // count = 3, Header.votes
    (104.toByte, 3.toByte) -> 299,  // count = 3, Header.parentId
    (104.toByte, 6.toByte) -> 296,  // count = 3, Header.transactionsRoot
    (101.toByte, 4.toByte) -> 293,  // count = 4, Context.INPUTS
    (100.toByte, 6.toByte) -> 288,  // count = 6, AvlTree.isUpdateAllowed
    (104.toByte, 5.toByte) -> 284,  // count = 3, Header.stateRoot
    (101.toByte, 7.toByte) -> 278,  // count = 1, Context.SELF
    (101.toByte, 5.toByte) -> 276,  // count = 1, Context.OUTPUTS
    (99.toByte, 8.toByte) -> 269,  // count = 163, Box.tokens
    (101.toByte, 10.toByte) -> 249,  // count = 2, Context.minerPubKey
    (12.toByte, 14.toByte) -> 238,  // count = 1725, SCollection.indices
    (101.toByte, 9.toByte) -> 182,  // count = 2, Context.LastBlockUtxoRootHash
    (106.toByte, 1.toByte) -> 169,  // count = 3, SigmaDslBuilder.groupGenerator
    (101.toByte, 6.toByte) -> 146,  // count = 1, Context.HEIGHT
  ).toMap
}
