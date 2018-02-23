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
    forAll(ehrAppendContractTransactionGen) { b: EhrContractTransaction =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("EhrContractTransaction serialization with validation afterwards") {
    forAll(ehrAppendContractTransactionGen) { b: EhrContractTransaction =>
      b.serializer.parseBytes(b.bytes).map(_.semanticValidity) shouldEqual Success(true)
    }
  }

  property("EhrContractTransaction validity") {
    forAll(ehrAppendContractTransactionGen) { b: EhrContractTransaction =>
      b.semanticValidity shouldBe true
    }
  }
}
