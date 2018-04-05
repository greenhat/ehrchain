package ehr.block

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class EhrBlockSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("EhrBlock validity") {
    forAll(ehrBlockGen) { b: EhrBlock =>
      b.validity shouldBe Success()
    }
  }

  property("EhrBlock serialization") {
    forAll(ehrBlockGen) { b: EhrBlock =>
      b.serializer.parseBytes(b.bytes).map( parsedB =>
        parsedB.bytes sameElements b.bytes
      ) shouldEqual Success(true)
    }
  }

}
