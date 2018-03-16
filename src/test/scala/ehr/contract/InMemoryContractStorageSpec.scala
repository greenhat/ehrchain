package ehr.contract

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class InMemoryContractStorageSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("add and retrieve one append contract") {
    val storage = new InMemoryContractStorage()
    forAll(ehrAppendContractUnlimitedGen) { b: AppendContract =>
      storage.add(Seq(b)).contractsForPatient[AppendContract](b.patientPK, b.providerPK) shouldEqual Seq(b)
    }
  }

  property("add and retrieve two append contracts") {
    val storage = new InMemoryContractStorage()
    forAll(ehrAppendContractUnlimitedGen) { b: AppendContract =>
      storage.add(Seq(b, b)).contractsForPatient[AppendContract](b.patientPK, b.providerPK) shouldEqual Seq(b, b)
    }
  }
}
