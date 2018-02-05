package com.ehrchain.transaction

import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import com.ehrchain.{EhrGenerators, core}

import scala.util.Success

class EhrTransactionSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrTransaction serialization") {
    forAll(ehrTransactionGen) { b: EhrTransaction =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("EhrTransaction validity") {
    forAll(ehrTransactionGen) { b: EhrTransaction =>
      b.validity.isSuccess shouldBe true
    }
  }

  property("invalid EhrTransaction(empty record)") {
    forAll(emptyRecordEhrTransactionGen) { b: EhrTransaction =>
      b.validity.isSuccess shouldBe false
    }
  }

  // todo test invalid signature
}
