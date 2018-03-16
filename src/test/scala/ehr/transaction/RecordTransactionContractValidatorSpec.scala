package ehr.transaction

import ehr.EhrGenerators
import ehr.contract._
import ehr.crypto.Curve25519KeyPair
import org.scalatest.{FlatSpec, Matchers}

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

  it should "be invalid if revoke append contract is present" in {
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val providerKeyPair: Curve25519KeyPair = key25519Gen.sample.get

    val appendContract = AppendContract(patientKeyPair.publicKey, providerKeyPair.publicKey, currentTimestamp, Unlimited)
    val revokeContract = RevokeAppendContract(patientKeyPair.publicKey, providerKeyPair.publicKey,
      currentTimestamp.plusSeconds(1), currentTimestamp.plusSeconds(1))
    val contractStorage = new InMemoryContractStorage().add(Seq[Contract](appendContract, revokeContract))
    val recordTx = EhrRecordTransactionCompanion.generate(patientKeyPair.publicKey,
      providerKeyPair, mockRecord, currentTimestamp.plusSeconds(2))

    val validator = new RecordTransactionContractValidator(contractStorage)
    validator.validity(recordTx) shouldBe false
  }
}
