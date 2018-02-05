package com.ehrchain.block

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class EhrBlockSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrBlock validity") {
    forAll(ehrBlockGen) { b: EhrBlock =>
      b.validity.isSuccess shouldBe true
    }
  }

  property("invalid EhrBlock(empty txs)") {
    forAll(zeroTxsEhrBlockGen) { b: EhrBlock =>
      b.validity.isSuccess shouldBe false
    }
  }

  // todo test invalid signature

}
