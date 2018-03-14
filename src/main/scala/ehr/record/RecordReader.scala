package ehr.record

import ehr.contract.ContractStorage
import ehr.core._
import ehr.crypto.{AesCipher, Curve25519KeyPair, EcdhDerivedKey}
import ehr.transaction.RecordTransactionStorage
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.util.Try

object RecordReader {

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def decryptRecordsInMemoryWithPatientKeys(patientKeys: Curve25519KeyPair,
                                            recordTxStorage: RecordTransactionStorage,
                                            recordFileStorage: RecordFileStorage):Seq[Try[FileSource]] =
    for {
      tx <- recordTxStorage.getByPatient(patientKeys.publicKey)
      recordFile <- tx.record.files
    } yield for {
      encryptedFileSource <- recordFileStorage.get(recordFile).toTry(s"no file source for $recordFile")
      decryptedBytes <- AesCipher.decryptInMemoryStream(encryptedFileSource.inputStream,
              EcdhDerivedKey.derivedKey(patientKeys, tx.generator))
    } yield ByteArrayFileSource(decryptedBytes)

  private def recordKeysForPatient(patientPK: PublicKey25519Proposition,
                                   providerKeys: Curve25519KeyPair,
                                   contractStorage: ContractStorage): Try[Map[PublicKey25519Proposition, KeyAes256]] =
    contractStorage.readContractsForPatient(patientPK)
      .filter(_.providerPK == providerKeys.publicKey)
      .map(_.decryptRecordKeysWithProviderSK(providerKeys.privateKey))
      .foldLeft(Try[Map[PublicKey25519Proposition, KeyAes256]](Map[PublicKey25519Proposition, KeyAes256]()))
      { (tryAccumKeys, tryRecordKeys) =>
        for {
          recordKeys <- tryRecordKeys
          keys <- tryAccumKeys
        } yield keys ++ recordKeys.keys
      }

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def decryptRecordsInMemoryWithProviderKeys(patientPK: PublicKey25519Proposition,
                                             providerKeys: Curve25519KeyPair,
                                             contractStorage: ContractStorage,
                                             recordTxStorage: RecordTransactionStorage,
                                             recordFileStorage: RecordFileStorage):Seq[Try[FileSource]] =
    for {
      tx <- recordTxStorage.getByPatient(patientPK)
      recordFile <- tx.record.files
    } yield for {
      recordKeys <- recordKeysForPatient(patientPK, providerKeys, contractStorage)
      recordKey <- recordKeys.get(tx.generator).toTry(s"no record key for ${tx.generator}")
      encryptedFileSource <- recordFileStorage.get(recordFile).toTry(s"no file source for $recordFile")
      decryptedBytes <- AesCipher.decryptInMemoryStream(encryptedFileSource.inputStream, recordKey)
    } yield ByteArrayFileSource(decryptedBytes)
}
