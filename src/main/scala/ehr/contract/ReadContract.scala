package ehr.contract

import java.time.Instant

import ehr.core.{EncryptedRecordKeys, KeyAes256}
import ehr.crypto.{AesCipher, Curve25519KeyPair, EcdhDerivedKey}
import ehr.serialization._
import ehr.transaction.RecordTransactionStorage
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519

import scala.util.{Success, Try}

@SerialVersionUID(0L)
final case class ReadContract(patientPK: PublicKey25519Proposition,
                              providerPK: PublicKey25519Proposition,
                              timestamp: Instant,
                              encryptedRecordKeys: EncryptedRecordKeys) extends Contract {
  override type M = ReadContract

  override def serializer: Serializer[M] = byteSerializer[M]

  override def semanticValidity: Try[Unit] = Success()

  def decryptRecordKeysWithProviderSK(providerSK: PrivateKey25519): Try[RecordKeys] =
    RecordKeys.decrypt((providerSK, providerPK), patientPK, encryptedRecordKeys)
}

object ReadContract {

  def generate(patientKeyPair: Curve25519KeyPair,
               providerPK: PublicKey25519Proposition,
               timestamp: Instant,
               recordKeys: RecordKeys): Try[ReadContract] =
    for {
      encryptedRecordKeys <- RecordKeys.encrypt(patientKeyPair, providerPK, recordKeys)
    } yield ReadContract(patientKeyPair.publicKey, providerPK, timestamp, encryptedRecordKeys)
}

final case class RecordKeys(keys: Map[PublicKey25519Proposition, KeyAes256])

@SerialVersionUID(0L)
object RecordKeys {

  def encrypt(patientKeyPair: Curve25519KeyPair,
              providerPK: PublicKey25519Proposition,
              recordKeys: RecordKeys): Try[EncryptedRecordKeys] =
    AesCipher.encryptInMemory(serializeToBytes(recordKeys),
      EcdhDerivedKey.derivedKey(patientKeyPair, providerPK)).map(EncryptedRecordKeys)

  def decrypt(providerKeyPair: Curve25519KeyPair,
              patientPK: PublicKey25519Proposition,
              encryptedRecordKeys: EncryptedRecordKeys): Try[RecordKeys] =
    for {
      decryptedBytes <- AesCipher.decryptInMemoryBytes(encryptedRecordKeys,
        EcdhDerivedKey.derivedKey(providerKeyPair, patientPK))
      recordKeys <- deserializeFromBytes[RecordKeys](decryptedBytes)
    } yield recordKeys

  def build(patientKeyPair: Curve25519KeyPair,
            recordTxStorage: RecordTransactionStorage): RecordKeys =
    RecordKeys(
      recordTxStorage.getByPatient(patientKeyPair.publicKey)
        .foldLeft(Map[PublicKey25519Proposition, KeyAes256]()) { case (keyMap, tx) =>
          if (keyMap.get(tx.generator).isDefined) keyMap
          else keyMap + (tx.generator -> EcdhDerivedKey.derivedKey(patientKeyPair, tx.generator))
        }
    )
}

