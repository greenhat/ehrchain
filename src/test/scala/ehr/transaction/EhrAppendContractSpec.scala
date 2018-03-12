package ehr.transaction

import ehr.EhrGenerators
import ehr.contract.EhrAppendContract
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class EhrAppendContractSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("unlimited term validity") {
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      b.validity.exists(_ == true) shouldBe true
    }
  }

  property("serialization") {
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("serialization with validation afterwards") {
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      b.serializer.parseBytes(b.bytes).map(_.validity.exists(_ == true)) shouldEqual Success(true)
    }
  }
}
