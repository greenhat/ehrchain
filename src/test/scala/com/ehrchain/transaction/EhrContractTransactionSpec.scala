package com.ehrchain.transaction

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class EhrContractTransactionSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrContractTransaction serialization") {
    // todo implement
  }

  property("EhrContractTransaction validity") {
    forAll(ehrAppendContractTransactionGen) { b: EhrContractTransaction =>
      b.validity shouldBe true
    }
  }

}
