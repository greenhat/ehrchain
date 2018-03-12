package ehr.contract

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class EhrInMemoryContractStorageSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("add and retrieve") {
    val storage = new EhrInMemoryContractStorage()
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      storage.add(Seq(b)).contractsForPatient(b.patientPK) shouldEqual Seq(b)
    }
  }
}
