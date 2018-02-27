package com.ehrchain.transaction

import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import com.ehrchain.{EhrGenerators, core}

import scala.util.Success

class EhrRecordTransactionSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrTransaction serialization") {
    forAll(ehrRecordTransactionGen) { b: EhrRecordTransaction =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("EhrTransaction validity") {
    forAll(ehrRecordTransactionGen) { b: EhrRecordTransaction =>
      b.semanticValidity shouldBe true
    }
  }

  // todo test invalid signature
}
