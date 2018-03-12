package ehr.transaction

import ehr.EhrGenerators
import ehr.contract.AppendContract
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class AppendContractSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("unlimited term validity") {
    forAll(ehrAppendContractUnlimitedGen) { b: AppendContract =>
      b.validity.exists(_ == true) shouldBe true
    }
  }

  property("serialization") {
    forAll(ehrAppendContractUnlimitedGen) { b: AppendContract =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("serialization with validation afterwards") {
    forAll(ehrAppendContractUnlimitedGen) { b: AppendContract =>
      b.serializer.parseBytes(b.bytes).map(_.validity.exists(_ == true)) shouldEqual Success(true)
    }
  }
}
