package ehr.transaction

import ehr.EhrGenerators
import ehr.contract.{AppendContract, InMemoryContractStorage, Unlimited}
import ehr.crypto.Curve25519KeyPair
import org.scalatest.{FlatSpec, Matchers}

// todo add revoke contract test

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
class RecordTransactionContractValidatorSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  "record transaction" should "be invalid if no append contract is present" in {
    val validator = new RecordTransactionContractValidator(new InMemoryContractStorage())
    validator.validity(ehrRecordTransactionGen.sample.get) shouldBe false
  }

  it should "be valid if append contract is active" in {
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val providerKeyPair: Curve25519KeyPair = key25519Gen.sample.get

    val appendContract = AppendContract(patientKeyPair.publicKey, providerKeyPair.publicKey, currentTimestamp, Unlimited)
    val contractStorage = new InMemoryContractStorage().add(Seq(appendContract))
    val recordTx = EhrRecordTransactionCompanion.generate(patientKeyPair.publicKey,
      providerKeyPair, mockRecord, currentTimestamp)

    val validator = new RecordTransactionContractValidator(contractStorage)
    validator.validity(recordTx) shouldBe true
  }
}
