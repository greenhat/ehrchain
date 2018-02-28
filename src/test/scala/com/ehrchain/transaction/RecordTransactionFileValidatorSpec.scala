package com.ehrchain.transaction

import com.ehrchain.EhrGenerators
import com.ehrchain.record.{InMemoryRecordFileStorage, InMemoryRecordFileStorageMock}
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}

class RecordTransactionFileValidatorSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("validate tx against an empty file storage") {
    val validator = new RecordTransactionFileValidator(new InMemoryRecordFileStorage())
    forAll(ehrRecordTransactionGen) { b: EhrRecordTransaction =>
      validator.validity(b) shouldBe false
    }
  }

  property("validate tx against an filled file storage") {
    val validator = new RecordTransactionFileValidator(InMemoryRecordFileStorageMock.storage)
    forAll(ehrRecordTransactionGen) { b: EhrRecordTransaction =>
      validator.validity(b) shouldBe true
    }
  }
}
