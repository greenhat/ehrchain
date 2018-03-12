package ehr.crypto

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class ECDHDerivedKeySpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("ECDH derived key equivalence for both parties") {
    forAll(key25519PairGen, key25519PairGen) { (party1Keys, party2Keys) =>
      ECDHDerivedKey.derivedKey(party1Keys, party2Keys.publicKey) shouldEqual
        ECDHDerivedKey.derivedKey(party2Keys, party1Keys.publicKey)
    }
  }

}
