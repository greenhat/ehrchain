package ehr

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.google.common.io.ByteStreams
import ehr.contract.{InMemoryContractStorage, ReadContract, RecordKeys}
import ehr.crypto.{AesCipher, Curve25519KeyPair, EcdhDerivedKey}
import ehr.record._
import ehr.transaction.{EhrRecordTransactionCompanion, InMemoryRecordTransactionStorage}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.OptionPartial", "org.wartremover.warts.TryPartial"))
class AccessRecordsSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  "Provider's appended record" should "be readable by the patient" in {
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val providerKeyPair: Curve25519KeyPair = key25519Gen.sample.get

    val recordFileContent = "health record".getBytes
    val encryptedRecordFileStream = new ByteArrayOutputStream()
    AesCipher.encrypt(new ByteArrayInputStream(recordFileContent),
      encryptedRecordFileStream,
      EcdhDerivedKey.derivedKey(providerKeyPair, patientKeyPair.publicKey)) shouldEqual Success()

    val recordFileSource = ByteArrayFileSource(encryptedRecordFileStream.toByteArray)
    val recordFile = FileHash.generate(recordFileSource).get

    val recordFileStorage = new InMemoryRecordFileStorage()
    recordFileStorage.put(recordFile, recordFileSource)

    val transactions = Seq(EhrRecordTransactionCompanion.generate(
      patientKeyPair.publicKey,
      providerKeyPair,
      Record(Seq(recordFile)),
      currentTimestamp))

    val recordTxStorage = new InMemoryRecordTransactionStorage().put(transactions)

    RecordReader.decryptRecordsInMemoryWithPatientKeys(patientKeyPair, recordTxStorage, recordFileStorage)
      .map(fileSource => ByteStreams.toByteArray(fileSource.get.inputStream))
      .exists(_ sameElements recordFileContent) shouldBe true
  }

  it should "be readable by another provider with access granted" in {
    val patientKeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val provider1KeyPair: Curve25519KeyPair = key25519Gen.sample.get

    val recordFileContent = "health record".getBytes
    val encryptedRecordFileStream = new ByteArrayOutputStream()
    AesCipher.encrypt(new ByteArrayInputStream(recordFileContent),
      encryptedRecordFileStream,
      EcdhDerivedKey.derivedKey(provider1KeyPair, patientKeyPair.publicKey)) shouldEqual Success()

    val recordFileSource = ByteArrayFileSource(encryptedRecordFileStream.toByteArray)
    val recordFile = FileHash.generate(recordFileSource).get

    val recordFileStorage = new InMemoryRecordFileStorage()
    recordFileStorage.put(recordFile, recordFileSource)

    val transactions = Seq(EhrRecordTransactionCompanion.generate(
      patientKeyPair.publicKey,
      provider1KeyPair,
      Record(Seq(recordFile)),
      currentTimestamp))

    val recordTxStorage = new InMemoryRecordTransactionStorage().put(transactions)

    val provider2KeyPair: Curve25519KeyPair = key25519Gen.sample.get
    val readContract = ReadContract.generate(patientKeyPair,
      provider2KeyPair.publicKey,
      currentTimestamp,
      RecordKeys.build(patientKeyPair, recordTxStorage)).get

    val contractStorage = new InMemoryContractStorage().add(Seq(readContract))

    RecordReader.decryptRecordsInMemoryWithProviderKeys(patientKeyPair.publicKey,
      provider2KeyPair,
      contractStorage,
      recordTxStorage,
      recordFileStorage)
      .map(fileSource => ByteStreams.toByteArray(fileSource.get.inputStream))
      .exists(_ sameElements recordFileContent) shouldBe true
  }
}
