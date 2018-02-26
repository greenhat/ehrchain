package com.ehrchain.transaction

import com.ehrchain.EhrGenerators
import com.ehrchain.contract.EhrInMemoryContractStorage
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Failed, Matchers, PropSpec}

import scala.util.Success

class EhrRecordTransactionContractValidatorSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("validate tx against an empty contract storage") {
    val validator = new EhrRecordTransactionContractValidator(new EhrInMemoryContractStorage())
    forAll(ehrRecordTransactionGen) { b: EhrRecordTransaction =>
      validator.validity(b) shouldBe false
    }
  }

  property("validate tx with valid contract in contract storage") {
    forAll(ehrTransactionPairGen) {
      case (contractTx: EhrContractTransaction) :: (recordTx: EhrRecordTransaction) :: Nil =>
          new EhrRecordTransactionContractValidator(
            new EhrInMemoryContractStorage().add(Seq(contractTx.contract)))
            .validity(recordTx) shouldBe true
      case _ => Failed(new Error("incorrect tx pair"))
    }
  }
}
