package com.ehrchain.crypto

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}
import scorex.crypto.encode.Base58

class ECDHDerivedKeySpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("ECDH derived key equivalence for both parties") {
    forAll(key25519PairGen, key25519PairGen) { (party1Keys, party2Keys) =>
      Base58.encode(ECDHDerivedKey.derivedKey(party1Keys, party2Keys.publicKey)) shouldEqual
        Base58.encode(ECDHDerivedKey.derivedKey(party2Keys, party1Keys.publicKey))
    }
  }

}
