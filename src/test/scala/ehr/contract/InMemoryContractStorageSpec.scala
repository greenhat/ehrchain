package ehr.contract

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class InMemoryContractStorageSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("add and retrieve") {
    val storage = new InMemoryContractStorage()
    forAll(ehrAppendContractUnlimitedGen) { b: AppendContract =>
      storage.add(Seq(b)).contractsForPatient(b.patientPK) shouldEqual Seq(b)
    }
  }
}
