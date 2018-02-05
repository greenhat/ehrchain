package com.ehrchain.block

import com.ehrchain.EhrGenerators
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}

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

}
