package com.ehrchain.transaction

import com.ehrchain.EhrGenerators
import com.ehrchain.contract.EhrInMemoryContractStorage
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers, PropSpec}

class EhrRecordTransactionContractValidatorSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("empty contract storage") {
    forAll(ehrRecordTransactionGen) { b: EhrRecordTransaction =>
      val validator = new EhrRecordTransactionContractValidator(new EhrInMemoryContractStorage())
      validator.validity(b) shouldBe true
    }
  }
}
