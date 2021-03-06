package ehr.transaction

import java.time.Instant

import com.google.common.primitives.Bytes
import ehr.serialization._
import ehr.contract.Contract
import ehr.crypto.Curve25519KeyPair
import io.circe.{Encoder, Json}
import io.circe.syntax._
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.PrivateKey25519Companion
import scorex.crypto.encode.Base58

@SerialVersionUID(0L)
final case class ContractTransaction(generator: PublicKey25519Proposition,
                                     signature: Signature25519,
                                     contract: Contract,
                                     timestamp: Instant) extends EhrTransaction {
  override type M = this.type

  override def serializer: Serializer[M] = byteSerializer[M]

  override def semanticValidity: Boolean =
    super.semanticValidity && contract.semanticValidity.isSuccess

  override val messageToSign: Array[Byte] =
    ContractTransaction.generateMessageToSign(timestamp, generator, contract)
}

object ContractTransaction {

  def generateMessageToSign(timestamp: Instant,
                            generator: PublicKey25519Proposition,
                            contract: Contract
                            ): Array[Byte] =
    Bytes.concat(
      serialize(timestamp),
      generator.bytes,
      contract.bytes
    )

  def generate(generatorKeys: Curve25519KeyPair,
               contract: Contract,
               timestamp: Instant): ContractTransaction = {
    val messageToSign = generateMessageToSign(timestamp, generatorKeys.publicKey, contract)
    val signature = PrivateKey25519Companion.sign(generatorKeys.privateKey, messageToSign)
    ContractTransaction(generatorKeys.publicKey, signature, contract, timestamp)
  }

  implicit val jsonEncoder: Encoder[ContractTransaction] = (tx: ContractTransaction) => {
    Map(
    "id" -> Base58.encode(tx.id).asJson,
    "timestamp" -> tx.timestamp.asJson,
    "generator" -> Base58.encode(tx.generator.bytes).asJson,
    //    "contract" -> contract.asJson,
    "signature" -> Base58.encode(tx.signature.bytes).asJson
    ).asJson
  }
}
