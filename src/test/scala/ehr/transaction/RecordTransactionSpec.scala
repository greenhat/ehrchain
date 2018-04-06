package ehr.transaction

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class RecordTransactionSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrTransaction serialization") {
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("EhrTransaction validity") {
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      b.semanticValidity shouldBe true
    }
  }
}
