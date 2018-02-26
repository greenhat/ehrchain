package com.ehrchain.contract

import com.ehrchain.EhrGenerators
import org.scalatest.{Matchers, PropSpec, Succeeded}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}

import scala.util.Success

class EhrInMemoryContractStorageSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("add") {
    val storage = new EhrInMemoryContractStorage()
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      storage.add(b) shouldBe Success(storage)
    }
  }

  property("retrieve") {
    val storage = new EhrInMemoryContractStorage()
    forAll(ehrAppendContractUnlimitedGen) { b: EhrAppendContract =>
      storage.add(b).map( _ =>
        storage.contractsForPatient(b.patientPK) shouldEqual Seq(b)
      ) shouldBe Success(Succeeded)
    }
  }
}
