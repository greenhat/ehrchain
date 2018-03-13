package ehr.contract

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import ehr.EhrGenerators
import ehr.crypto.{AESCipher, Curve25519KeyPair, ECDHDerivedKey}
import ehr.record._
import ehr.transaction.{EhrRecordTransactionCompanion, InMemoryRecordTransactionStorage}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.OptionPartial", "org.wartremover.warts.TryPartial"))
class ReadContractSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  "Record keys" should "be built from existing record transaction" in {
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val providerKeyPair: Curve25519KeyPair = key25519Gen.sample.get

    val encryptedRecordFileStream = new ByteArrayOutputStream()
    AESCipher.encrypt(new ByteArrayInputStream("health record".getBytes),
      encryptedRecordFileStream,
      ECDHDerivedKey.derivedKey(providerKeyPair, patientKeyPair.publicKey)) shouldEqual Success()

    val recordFile = RecordFile.generate(ByteArrayFileSource(encryptedRecordFileStream.toByteArray)).get

    val transactions = Seq(EhrRecordTransactionCompanion.generate(
      patientKeyPair.publicKey,
      providerKeyPair,
      Record(Seq(recordFile)),
      currentTimestamp))

    val recordTxStorage = new InMemoryRecordTransactionStorage().put(transactions)
    val expectedRecordKeys = RecordKeys(
      Map(providerKeyPair.publicKey -> ECDHDerivedKey.derivedKey(patientKeyPair, providerKeyPair.publicKey)))

    RecordKeys.build(patientKeyPair, recordTxStorage) shouldEqual expectedRecordKeys
  }
}
