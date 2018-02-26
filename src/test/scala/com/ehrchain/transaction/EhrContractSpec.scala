package com.ehrchain.transaction

import com.ehrchain.EhrGenerators
import com.ehrchain.contract.EhrAppendContract
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class EhrContractSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrAppendContract(unlimited term) validity") {
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      b.validity.exists(_ == true) shouldBe true
    }
  }

  property("EhrAppendContract serialization") {
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      b.serializer.parseBytes(b.bytes).map( _.bytes sameElements b.bytes) shouldEqual Success(true)
    }
  }

  property("EhrAppendContract serialization with validation afterwards") {
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      b.serializer.parseBytes(b.bytes).map(_.validity.exists(_ == true)) shouldEqual Success(true)
    }
  }
}
