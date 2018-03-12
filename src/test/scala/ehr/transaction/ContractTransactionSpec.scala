package ehr.transaction

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class ContractTransactionSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrContractTransaction serialization") {
    forAll(ehrAppendContractTransactionGen) { b: ContractTransaction =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("EhrContractTransaction serialization with validation afterwards") {
    forAll(ehrAppendContractTransactionGen) { b: ContractTransaction =>
      b.serializer.parseBytes(b.bytes).map(_.semanticValidity) shouldEqual Success(true)
    }
  }

  property("EhrContractTransaction validity") {
    forAll(ehrAppendContractTransactionGen) { b: ContractTransaction =>
      b.semanticValidity shouldBe true
    }
  }
}
