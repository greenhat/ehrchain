package ehr.transaction

import java.time.Instant

import com.google.common.primitives.Bytes
import ehr.serialization._
import ehr.contract.EhrContract
import ehr.crypto.Curve25519KeyPair
import io.circe.Json
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.crypto.encode.Base58

@SerialVersionUID(0L)
final case class EhrContractTransaction(generator: PublicKey25519Proposition,
                                        signature: Signature25519,
                                        contract: EhrContract,
                                        timestamp: Instant) extends EhrTransaction {
  override type M = this.type

  override def serializer: Serializer[M] = byteSerializer[M]

  override def json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "timestamp" -> timestamp.asJson,
    "generator" -> Base58.encode(generator.bytes).asJson,
//    "contract" -> contract.asJson,
    "signature" -> Base58.encode(signature.bytes).asJson,
  ).asJson

  override def semanticValidity: Boolean =
    super.semanticValidity && contract.validity.exists(_ == true)

  override val messageToSign: Array[Byte] =
    EhrContractTransaction.generateMessageToSign(timestamp, generator, contract)
}

object EhrContractTransaction {

  def generateMessageToSign(timestamp: Instant,
                            generator: PublicKey25519Proposition,
                            contract: EhrContract
                            ): Array[Byte] =
    Bytes.concat(
      serialize(timestamp),
      generator.bytes,
      contract.bytes
    )

  def generate(generatorKeys: Curve25519KeyPair,
               contract: EhrContract,
               timestamp: Instant): EhrContractTransaction = {
    val messageToSign = generateMessageToSign(timestamp, generatorKeys.publicKey, contract)
    val signature = PrivateKey25519Companion.sign(generatorKeys.privateKey, messageToSign)
    EhrContractTransaction(generatorKeys.publicKey, signature, contract, timestamp)
  }
}
