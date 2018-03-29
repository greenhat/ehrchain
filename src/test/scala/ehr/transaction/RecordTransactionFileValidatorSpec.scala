package ehr.transaction

import ehr.EhrGenerators
import ehr.record.{InMemoryRecordFileStorage, InMemoryRecordFileStorageMock}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class RecordTransactionFileValidatorSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("validate tx against an empty file storage") {
    val validator = new RecordTransactionFileValidator(new InMemoryRecordFileStorage())
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      validator.validity(b) shouldBe false
    }
  }

  property("validate tx against an filled file storage") {
    val validator = new RecordTransactionFileValidator(InMemoryRecordFileStorageMock.storage)
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      validator.validity(b) shouldBe true
    }
  }

  property("no missing files") {
    val validator = new RecordTransactionFileValidator(InMemoryRecordFileStorageMock.storage)
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      validator.findMissingFiles(Seq(b)).isEmpty shouldBe true
    }
  }

  property("missing file (empty file storage)") {
    val validator = new RecordTransactionFileValidator(new InMemoryRecordFileStorage())
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      validator.findMissingFiles(Seq(b)).isEmpty shouldBe false
    }
  }
}
