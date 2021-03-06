package wrappers.java.math {
  import scalan._

  import impl._

  import special.sigma.wrappers.WrappersModule

  import special.sigma.wrappers.BigIntegerWrapSpec

  trait WBigIntegers extends Base { self: WrappersModule =>
    import WArray._;
    import WBigInteger._;
    @External("BigInteger") @Liftable trait WBigInteger extends Def[WBigInteger] {
      @External def longValueExact: Rep[Long];
      @External def intValueExact: Rep[Int];
      @External def shortValueExact: Rep[Short];
      @External def byteValueExact: Rep[Byte];
      @External def longValue: Rep[Long];
      @External def intValue: Rep[Int];
      @External def shortValue: Rep[Short];
      @External def byteValue: Rep[Byte];
      @External def signum: Rep[Int];
      @External def negate: Rep[WBigInteger];
      @External def abs: Rep[WBigInteger];
      @External def shiftRight(x$1: Rep[Int]): Rep[WBigInteger];
      @External def shiftLeft(x$1: Rep[Int]): Rep[WBigInteger];
      @External def isProbablePrime(x$1: Rep[Int]): Rep[Boolean];
      @External def bitLength: Rep[Int];
      @External def bitCount: Rep[Int];
      @External def getLowestSetBit: Rep[Int];
      @External def flipBit(x$1: Rep[Int]): Rep[WBigInteger];
      @External def clearBit(x$1: Rep[Int]): Rep[WBigInteger];
      @External def setBit(x$1: Rep[Int]): Rep[WBigInteger];
      @External def testBit(x$1: Rep[Int]): Rep[Boolean];
      @External def pow(x$1: Rep[Int]): Rep[WBigInteger];
      @External def andNot(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def not: Rep[WBigInteger];
      @External def xor(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def or(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def and(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def gcd(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def max(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def min(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def compareTo(x$1: Rep[WBigInteger]): Rep[Int];
      @External def divide(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def remainder(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def modPow(x$1: Rep[WBigInteger], x$2: Rep[WBigInteger]): Rep[WBigInteger];
      @External def modInverse(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def mod(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def multiply(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def subtract(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def add(x$1: Rep[WBigInteger]): Rep[WBigInteger];
      @External def toByteArray: Rep[WArray[Byte]]
    };
    trait WBigIntegerCompanion {
      @Constructor @OverloadId(value = "constructor_1") def apply(x$1: Rep[Int], x$2: Rep[WArray[Byte]]): Rep[WBigInteger];
      @Constructor @OverloadId(value = "constructor_2") def apply(x$1: Rep[String]): Rep[WBigInteger];
      @External def valueOf(x$1: Rep[Long]): Rep[WBigInteger]
    }
  }
}