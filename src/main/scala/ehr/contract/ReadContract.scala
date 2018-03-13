package ehr.contract

import java.time.Instant

import ehr.core.{EncryptedRecordKeys, KeyAes256}
import ehr.crypto.Curve25519KeyPair
import ehr.serialization._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519

import scala.util.Try

@SerialVersionUID(0L)
final case class ReadContract(patientPK: PublicKey25519Proposition,
                              providerPK: PublicKey25519Proposition,
                              timestamp: Instant,
                              encryptedRecordKeys: EncryptedRecordKeys) extends Contract {
  override type M = ReadContract

  override def serializer: Serializer[M] = byteSerializer[M]

  override def semanticValidity: Try[Unit] = ???

  def decryptRecordKeys(providerSK: PrivateKey25519): RecordKeys =
    RecordKeys.decrypt((providerSK, providerPK), patientPK, encryptedRecordKeys)
}

object ReadContract {

  def generate(patientKeyPair: Curve25519KeyPair,
               providerPK: PublicKey25519Proposition,
               timestamp: Instant,
               recordKeys: RecordKeys): ReadContract =
    ReadContract(patientKeyPair.publicKey,
      providerPK,
      timestamp,
      RecordKeys.encrypt(patientKeyPair, providerPK, recordKeys))
}

final case class RecordKeys(keys: Map[PublicKey25519Proposition, KeyAes256])

object RecordKeys {

  def encrypt(patientKeyPair: Curve25519KeyPair,
              providerPK: PublicKey25519Proposition,
              recordKeys: RecordKeys): EncryptedRecordKeys = ???

  def decrypt(providerKeyPair: Curve25519KeyPair,
              patientPK: PublicKey25519Proposition,
              encryptedRecordKeys: EncryptedRecordKeys): RecordKeys = ???
}

