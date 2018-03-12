package ehr.transaction

import ehr.EhrGenerators
import ehr.contract.InMemoryContractStorage
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Failed, Matchers, PropSpec}

class RecordTransactionContractValidatorSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("validate tx against an empty contract storage") {
    val validator = new EhrRecordTransactionContractValidator(new InMemoryContractStorage())
    forAll(ehrRecordTransactionGen) { b: RecordTransaction =>
      validator.validity(b) shouldBe false
    }
  }

  property("validate tx with valid contract in contract storage") {
    forAll(ehrTransactionPairGen) {
      case (contractTx: ContractTransaction) :: (recordTx: RecordTransaction) :: Nil =>
          new EhrRecordTransactionContractValidator(
            new InMemoryContractStorage().add(Seq(contractTx.contract)))
            .validity(recordTx) shouldBe true
      case _ => Failed(new Error("incorrect tx pair"))
    }
  }
}
