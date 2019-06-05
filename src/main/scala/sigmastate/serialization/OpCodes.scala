package sigmastate.serialization

/** Encoding of types for serialization. */
trait TypeCodes {
  /** Decoding of types depends on the first byte and in general is a recursive procedure
    * consuming some number of bytes from Reader.
    * All data types are recognized by the first byte falling in the region [FirstDataType .. LastDataType] */
  val FirstDataType: Byte = 1.toByte
  val LastDataType : Byte = 111.toByte

  /** SFunc types occupy remaining space of byte values [FirstFuncType .. 255] */
  val FirstFuncType: Byte = (LastDataType + 1).toByte
  val LastFuncType : Byte = 255.toByte
}

/** Encoding of values for serialization. */
trait ValueCodes extends TypeCodes {
  /** We use optimized encoding of constant values to save space in serialization.
    * Since Box registers are stored as Constant nodes we save 1 byte for each register.
    * This is due to convention that Value.opCode falling in [1..LastDataType] region is a constant.
    * Thus, we can just decode an instance of SType and then decode data using DataSerializer.
    *
    * Decoding of constants depends on the first byte and in general is a recursive procedure
    * consuming some number of bytes from Reader.
    * */
  val ConstantCode: Byte = 0

  /** The last constant code is equal to FirstFuncType which represent generic function type.
    * We use this single code to represent all functional constants, since we don't have enough space in single byte.
    * Subsequent bytes have to be read from Reader in order to decode the type of the function and the corresponding data. */
  val LastConstantCode: Byte = (LastDataType + 1).toByte
}

/** The set of all possible IR graph nodes can be split in two subsets:
  * 1) operations which may appear in ErgoTree (these are defined by `OpCodes` below)
  * 2) operations which are not valid to be in ErgoTree, but serve special purposes. (these are defined by `OpCodesExtra`)
  * We can assume they are both Byte-sized codes, and store as a single byte, but as long as we can differentiate them
  * from context (Where we cannot, we should use special encoding).
  *
  * The general extended encoding is like the following:
  * 0-255 - range of OpCodes
  * 256-511 - range of OpCodesExtra
  * Thus, any code in an extended code range of 0-511 can be saved using `putUShort`.
  * We use Byte to represent OpCodes and OpCodesExtra.
  * We use Short to represent any op code from extended code range.
  * And we use VLQ to serialize Short values of extended codes.
  *
  * Examples:
  * 1) For validation rule CheckValidOpCode we use OpCodes range, so we use single byte encoding.
  * 2) For CheckCostFuncOperation we use 1-511 range and extended encoding (see docs)
  */
object OpCodes extends ValueCodes {

  type OpCode = Byte
  type OpCodeExtra = Short

  // serialization is not required
  val Undefined: OpCode = 0: Byte

  // variables
  val TaggedVariableCode: OpCode = (LastConstantCode + 1).toByte
  val ValUseCode: OpCode = (LastConstantCode + 2).toByte
  val ConstantPlaceholderCode: OpCode = (LastConstantCode + 3).toByte
  val SubstConstantsCode: OpCode = (LastConstantCode + 4).toByte // reserved 5 - 9 (5)

  val LongToByteArrayCode  : OpCode = (LastConstantCode + 10).toByte
  val ByteArrayToBigIntCode: OpCode = (LastConstantCode + 11).toByte
  val ByteArrayToLongCode  : OpCode = (LastConstantCode + 12).toByte
  val DowncastCode         : OpCode = (LastConstantCode + 13).toByte
  val UpcastCode           : OpCode = (LastConstantCode + 14).toByte

  // EvaluatedValue descendants
  val TrueCode              : OpCode = (LastConstantCode + 15).toByte
  val FalseCode             : OpCode = (LastConstantCode + 16).toByte
  val UnitConstantCode      : OpCode = (LastConstantCode + 17).toByte
  val GroupGeneratorCode    : OpCode = (LastConstantCode + 18).toByte
  val ConcreteCollectionCode: OpCode = (LastConstantCode + 19).toByte // reserved 20 (1)
  val ConcreteCollectionBooleanConstantCode: OpCode = (LastConstantCode + 21).toByte

  val TupleCode      : OpCode = (LastConstantCode + 22).toByte
  val Select1Code    : OpCode = (LastConstantCode + 23).toByte
  val Select2Code    : OpCode = (LastConstantCode + 24).toByte
  val Select3Code    : OpCode = (LastConstantCode + 25).toByte
  val Select4Code    : OpCode = (LastConstantCode + 26).toByte
  val Select5Code    : OpCode = (LastConstantCode + 27).toByte
  val SelectFieldCode: OpCode = (LastConstantCode + 28).toByte // reserved 29-30 (2)

  // Relation descendants
  val LtCode     : OpCode = (LastConstantCode + 31).toByte
  val LeCode     : OpCode = (LastConstantCode + 32).toByte
  val GtCode     : OpCode = (LastConstantCode + 33).toByte
  val GeCode     : OpCode = (LastConstantCode + 34).toByte
  val EqCode     : OpCode = (LastConstantCode + 35).toByte
  val NeqCode    : OpCode = (LastConstantCode + 36).toByte
  val IfCode     : OpCode = (LastConstantCode + 37).toByte
  val AndCode    : OpCode = (LastConstantCode + 38).toByte
  val OrCode     : OpCode = (LastConstantCode + 39).toByte
  val AtLeastCode: OpCode = (LastConstantCode + 40).toByte

  // Arithmetic codes
  val MinusCode        : OpCode = (LastConstantCode + 41).toByte
  val PlusCode         : OpCode = (LastConstantCode + 42).toByte
  val XorCode          : OpCode = (LastConstantCode + 43).toByte
  val MultiplyCode     : OpCode = (LastConstantCode + 44).toByte
  val DivisionCode     : OpCode = (LastConstantCode + 45).toByte
  val ModuloCode       : OpCode = (LastConstantCode + 46).toByte
  val ExponentiateCode : OpCode = (LastConstantCode + 47).toByte
  val MultiplyGroupCode: OpCode = (LastConstantCode + 48).toByte
  val MinCode          : OpCode = (LastConstantCode + 49).toByte
  val MaxCode          : OpCode = (LastConstantCode + 50).toByte

  // Environment codes
  val HeightCode               : OpCode = (LastConstantCode + 51).toByte
  val InputsCode               : OpCode = (LastConstantCode + 52).toByte
  val OutputsCode              : OpCode = (LastConstantCode + 53).toByte
  val LastBlockUtxoRootHashCode: OpCode = (LastConstantCode + 54).toByte
  val SelfCode                 : OpCode = (LastConstantCode + 55).toByte  // reserved 56 - 59 (4)

  val MinerPubkeyCode          : OpCode = (LastConstantCode + 60).toByte

  // Collection and tree operations codes
  val MapCollectionCode    : OpCode = (LastConstantCode + 61).toByte
  val ExistsCode           : OpCode = (LastConstantCode + 62).toByte
  val ForAllCode           : OpCode = (LastConstantCode + 63).toByte
  val FoldCode             : OpCode = (LastConstantCode + 64).toByte
  val SizeOfCode           : OpCode = (LastConstantCode + 65).toByte
  val ByIndexCode          : OpCode = (LastConstantCode + 66).toByte
  val AppendCode           : OpCode = (LastConstantCode + 67).toByte
  val SliceCode            : OpCode = (LastConstantCode + 68).toByte
  val FilterCode           : OpCode = (LastConstantCode + 69).toByte
  val AvlTreeCode          : OpCode = (LastConstantCode + 70).toByte
  val AvlTreeGetCode       : OpCode = (LastConstantCode + 71).toByte  // reserved 72 - 80 (9)

  // Type casts codes
  val ExtractAmountCode        : OpCode = (LastConstantCode + 81).toByte
  val ExtractScriptBytesCode   : OpCode = (LastConstantCode + 82).toByte
  val ExtractBytesCode         : OpCode = (LastConstantCode + 83).toByte
  val ExtractBytesWithNoRefCode: OpCode = (LastConstantCode + 84).toByte
  val ExtractIdCode            : OpCode = (LastConstantCode + 85).toByte
  val ExtractRegisterAs        : OpCode = (LastConstantCode + 86).toByte
  val ExtractCreationInfoCode  : OpCode = (LastConstantCode + 87).toByte // reserved 88 - 90 (3)

  // Cryptographic operations codes
  val CalcBlake2b256Code         : OpCode = (LastConstantCode + 91).toByte
  val CalcSha256Code             : OpCode = (LastConstantCode + 92).toByte
  val ProveDlogCode              : OpCode = (LastConstantCode + 93).toByte
  val ProveDHTupleCode           : OpCode = (LastConstantCode + 94).toByte
  val SigmaPropIsProvenCode      : OpCode = (LastConstantCode + 95).toByte
  val SigmaPropBytesCode         : OpCode = (LastConstantCode + 96).toByte
  val BoolToSigmaPropCode        : OpCode = (LastConstantCode + 97).toByte
  // we don't rely on this yet but it's nice to have TrivialPropFalseCode.toUByte < TrivialPropTrueCode.toUByte
  val TrivialPropFalseCode       : OpCode = (LastConstantCode + 98).toByte
  val TrivialPropTrueCode        : OpCode = (LastConstantCode + 99).toByte

  // Deserialization codes
  val DeserializeContextCode : OpCode = (LastConstantCode + 100).toByte
  val DeserializeRegisterCode: OpCode = (LastConstantCode + 101).toByte  // Block codes
  val ValDefCode: OpCode = (LastConstantCode + 102).toByte
  val FunDefCode: OpCode = (LastConstantCode + 103).toByte
  val BlockValueCode: OpCode = (LastConstantCode + 104).toByte
  val FuncValueCode: OpCode = (LastConstantCode + 105).toByte
  val FuncApplyCode: OpCode = (LastConstantCode + 106).toByte
  val PropertyCallCode: OpCode = (LastConstantCode + 107).toByte
  val MethodCallCode: OpCode = (LastConstantCode + 108).toByte
  val GlobalCode    : OpCode = (LastConstantCode + 109).toByte

  val SomeValueCode: OpCode = (LastConstantCode + 110).toByte
  val NoneValueCode: OpCode = (LastConstantCode + 111).toByte  // reserved 112 - 114 (3)

  val GetVarCode         : OpCode = (LastConstantCode + 115).toByte
  val OptionGetCode      : OpCode = (LastConstantCode + 116).toByte
  val OptionGetOrElseCode: OpCode = (LastConstantCode + 117).toByte
  val OptionIsDefinedCode: OpCode = (LastConstantCode + 118).toByte

  // Modular arithmetic operations codes
  val ModQCode     : OpCode = (LastConstantCode + 119).toByte
  val PlusModQCode : OpCode = (LastConstantCode + 120).toByte
  val MinusModQCode: OpCode = (LastConstantCode + 121).toByte

  val SigmaAndCode : OpCode = (LastConstantCode + 122).toByte
  val SigmaOrCode  : OpCode = (LastConstantCode + 123).toByte
  val BinOrCode    : OpCode = (LastConstantCode + 124).toByte
  val BinAndCode   : OpCode = (LastConstantCode + 125).toByte

  val DecodePointCode: OpCode = (LastConstantCode + 126).toByte

  val LogicalNotCode : OpCode = (LastConstantCode + 127).toByte
  val NegationCode   : OpCode = (LastConstantCode + 128).toByte
  val BitInversionCode   : OpCode = (LastConstantCode + 129).toByte
  val BitOrCode      : OpCode = (LastConstantCode + 130).toByte
  val BitAndCode     : OpCode = (LastConstantCode + 131).toByte

  val BinXorCode     : OpCode = (LastConstantCode + 132).toByte

  val BitXorCode     : OpCode = (LastConstantCode + 133).toByte
  val BitShiftRightCode    : OpCode = (LastConstantCode + 134).toByte
  val BitShiftLeftCode     : OpCode = (LastConstantCode + 135).toByte
  val BitShiftRightZeroedCode     : OpCode = (LastConstantCode + 136).toByte

  val CollShiftRightCode    : OpCode = (LastConstantCode + 137).toByte
  val CollShiftLeftCode     : OpCode = (LastConstantCode + 138).toByte
  val CollShiftRightZeroedCode     : OpCode = (LastConstantCode + 139).toByte

  val CollRotateLeftCode     : OpCode = (LastConstantCode + 140).toByte
  val CollRotateRightCode     : OpCode = (LastConstantCode + 141).toByte

  val ContextCode             : OpCode = (LastConstantCode + 142).toByte
  val XorOfCode               : OpCode = (LastConstantCode + 143).toByte // equals to 255

  // OpCodesExtra (range of 256-511)

  val FirstOpCodeExtraCode = 256

  val OpCostCode: OpCodeExtra         = (FirstOpCodeExtraCode + 1).toShort
  val PerKbCostOfCode: OpCodeExtra    = (FirstOpCodeExtraCode + 2).toShort
  val CastCode: OpCodeExtra           = (FirstOpCodeExtraCode + 3).toShort
  val IntPlusMonoidCode: OpCodeExtra  = (FirstOpCodeExtraCode + 4).toShort
  val ThunkDefCode: OpCodeExtra       = (FirstOpCodeExtraCode + 5).toShort
  val SCMInputsCode: OpCodeExtra      = (FirstOpCodeExtraCode + 6).toShort
  val SCMOutputsCode: OpCodeExtra     = (FirstOpCodeExtraCode + 7).toShort
  val SCMDataInputsCode: OpCodeExtra  = (FirstOpCodeExtraCode + 8).toShort
  val SCMSelfBoxCode: OpCodeExtra     = (FirstOpCodeExtraCode + 9).toShort
  val SCMLastBlockUtxoRootHashCode: OpCodeExtra = (FirstOpCodeExtraCode + 10).toShort
  val SCMHeadersCode: OpCodeExtra     = (FirstOpCodeExtraCode + 11).toShort
  val SCMPreHeaderCode: OpCodeExtra   = (FirstOpCodeExtraCode + 12).toShort
  val SCMGetVarCode: OpCodeExtra      = (FirstOpCodeExtraCode + 13).toShort
  val SBMPropositionBytesCode: OpCodeExtra = (FirstOpCodeExtraCode + 14).toShort
  val SBMBytesCode: OpCodeExtra       = (FirstOpCodeExtraCode + 15).toShort
  val SBMBytesWithoutRefCode: OpCodeExtra = (FirstOpCodeExtraCode + 16).toShort
  val SBMRegistersCode: OpCodeExtra   = (FirstOpCodeExtraCode + 17).toShort
  val SBMGetRegCode: OpCodeExtra      = (FirstOpCodeExtraCode + 18).toShort
  val SBMTokensCode: OpCodeExtra      = (FirstOpCodeExtraCode + 19).toShort
  val SSPMPropBytesCode: OpCodeExtra  = (FirstOpCodeExtraCode + 20).toShort
  val SAVMTValCode: OpCodeExtra       = (FirstOpCodeExtraCode + 21).toShort
  val SAVMValueSizeCode: OpCodeExtra  = (FirstOpCodeExtraCode + 22).toShort
  val SizeMDataSizeCode: OpCodeExtra  = (FirstOpCodeExtraCode + 23).toShort
  val SPairLCode: OpCodeExtra         = (FirstOpCodeExtraCode + 24).toShort
  val SPairRCode: OpCodeExtra         = (FirstOpCodeExtraCode + 25).toShort
  val SCollMSizesCode: OpCodeExtra    = (FirstOpCodeExtraCode + 26).toShort
  val SOptMSizeOptCode: OpCodeExtra   = (FirstOpCodeExtraCode + 27).toShort
  val SFuncMSizeEnvCode: OpCodeExtra  = (FirstOpCodeExtraCode + 28).toShort
  val CSizePairCtorCode: OpCodeExtra  = (FirstOpCodeExtraCode + 29).toShort
  val CSizeFuncCtorCode: OpCodeExtra  = (FirstOpCodeExtraCode + 30).toShort
  val CSizeOptionCtorCode: OpCodeExtra = (FirstOpCodeExtraCode + 31).toShort
  val CSizeCollCtorCode: OpCodeExtra  = (FirstOpCodeExtraCode + 32).toShort
  val CSizeBoxCtorCode: OpCodeExtra   = (FirstOpCodeExtraCode + 33).toShort
  val CSizeContextCtorCode: OpCodeExtra = (FirstOpCodeExtraCode + 34).toShort
  val CSizeAnyValueCtorCode: OpCodeExtra = (FirstOpCodeExtraCode + 35).toShort
  val CReplCollCtorCode: OpCodeExtra  = (FirstOpCodeExtraCode + 36).toShort
  val CollMSumCode: OpCodeExtra       = (FirstOpCodeExtraCode + 37).toShort
  val PairOfColsCtorCode: OpCodeExtra = (FirstOpCodeExtraCode + 37).toShort
  val CBMReplicateCode: OpCodeExtra   = (FirstOpCodeExtraCode + 38).toShort
  val CBMFromItemsCode: OpCodeExtra   = (FirstOpCodeExtraCode + 39).toShort
  val CostOfCode: OpCodeExtra         = (FirstOpCodeExtraCode + 40).toShort
  val UOSizeOfCode: OpCodeExtra       = (FirstOpCodeExtraCode + 41).toShort
  val SPCMSomeCode: OpCodeExtra       = (FirstOpCodeExtraCode + 42).toShort
}
