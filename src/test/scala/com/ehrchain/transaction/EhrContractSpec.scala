package com.ehrchain.transaction

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class EhrContractSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrAppendContract(unlimited term validity") {
    forAll(ehrAppendContractGen) { b: EhrAppendContract =>
      b.validity.exists(_ == true) shouldBe true
    }
  }

}
