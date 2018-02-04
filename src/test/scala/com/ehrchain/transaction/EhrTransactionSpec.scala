package com.ehrchain.transaction

import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import com.ehrchain.{EhrGenerators, core}

class EhrTransactionSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrTransaction serialization") {
    forAll(ehrTransactionGen) { b: EhrTransaction =>
      val parsed = b.serializer.parseBytes(b.bytes).get
      parsed.bytes shouldEqual b.bytes
    }
  }

  property("EhrTransaction validity") {
    forAll(ehrTransactionGen) { b: EhrTransaction =>
      b.validity.isSuccess shouldBe true
    }
  }
}
