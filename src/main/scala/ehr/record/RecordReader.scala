package ehr.record

import ehr.crypto.{AESCipher, Curve25519KeyPair, ECDHDerivedKey}
import ehr.transaction.RecordTransactionStorage
import ehr.core._

import scala.util.Try

object RecordReader {

  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def decryptPatientRecordsInMemory(patientKeys: Curve25519KeyPair,
                                    recordTxStorage: RecordTransactionStorage,
                                    recordFileStorage: RecordFileStorage):Seq[Try[FileSource]] =
    for {
      tx <- recordTxStorage.getBySubject(patientKeys.publicKey)
      recordFile <- tx.record.files
    } yield for {
      encryptedFileSource <- recordFileStorage.get(recordFile).toTry(s"no file source for $recordFile")
      decryptedBytes <- AESCipher.decryptInMemory(encryptedFileSource.inputStream,
              ECDHDerivedKey.derivedKey(patientKeys, tx.generator))
    } yield ByteArrayFileSource(decryptedBytes)
}
