package ehr.block

import java.time.Instant

import com.google.common.primitives.{Bytes, Ints}
import ehr.transaction.{ContractTransaction, EhrTransaction}
import ehr.serialization._
import examples.commons.Nonce
import io.circe.{Encoder, Json}
import io.circe.syntax._
import scorex.core.block.Block
import scorex.core.block.Block.{BlockId, Timestamp, Version}
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256

import scala.annotation.tailrec
import scala.util.Random

final class EhrBlock(val parentId: BlockId,
                     val dateTime: Instant,
                     val nonce: Nonce,
                     val transactions: Seq[EhrTransaction],
                     val signature: Signature25519,
                     val generator: PublicKey25519Proposition,
                     val difficulty: Int)
  extends Block[PublicKey25519Proposition, EhrTransaction] {

  require(transactions.nonEmpty)

  override type M = EhrBlock

  override lazy val modifierTypeId: ModifierTypeId = EhrBlock.ModifierType

  override def version: Version = 1: Byte

  override def timestamp: Timestamp = dateTime.toEpochMilli

  override def toString: String = s"EhrBlock(${this.asJson.noSpaces}})"

  override def id: ModifierId =
    ModifierId @@ Blake2b256(parentId ++ serialize(dateTime) ++ generator.bytes)

  override def serializer: Serializer[M] = byteSerializer[M]

  lazy val validity: Boolean =
    transactions.nonEmpty &&
      signature.isValid(generator, EhrBlock.generateMessageToSign(parentId, dateTime, nonce, transactions, generator, difficulty)) &&
      powValidity

  lazy val powValidity: Boolean = {
    (difficulty == 0) || Blake2b256(bytes).startsWith(Array.fill[Byte](difficulty)(0))
  }
}

object EhrBlock {

  val MaxBlockSize: Int = 512 * 1024  //512K
  val ModifierType: ModifierTypeId = ModifierTypeId @@ 1.toByte

  def generateMessageToSign(parentId: BlockId,
                            timestamp: Instant,
                            nonce: Nonce,
                            transactions: Seq[EhrTransaction],
                            generator: PublicKey25519Proposition,
                            difficulty: Int): Array[Byte] =
    Bytes.concat(
      parentId,
      serialize(timestamp),
      serialize(nonce),
      transactions.flatMap(_.bytes).toArray,
      serialize(generator),
      Ints.toByteArray(difficulty),
    )

  @tailrec
  def generate(parentId: BlockId,
               timestamp: Instant,
               transactions: Seq[EhrTransaction],
               generatorKeys: (PrivateKey25519, PublicKey25519Proposition),
               difficulty: Int): EhrBlock = {
    val generatorSK = generatorKeys._1
    val generatorPK = generatorKeys._2
    val nonce = Nonce @@ Random.nextLong()
    val msgToSign = EhrBlock.generateMessageToSign(parentId, timestamp, nonce, transactions,
      generatorPK, difficulty)
    val signature = PrivateKey25519Companion.sign(generatorSK, msgToSign)
    val block = new EhrBlock(parentId, timestamp, nonce, transactions, signature, generatorPK, difficulty)
    if (block.validity) block
    else generate(parentId, timestamp, transactions, generatorKeys, difficulty)
  }

  implicit val jsonEncoder: Encoder[EhrBlock] = (block: EhrBlock) => {
    Map(
      "id" -> Base58.encode(block.id).asJson,
      "parentId" -> Base58.encode(block.parentId).asJson,
      "timestamp" -> block.dateTime.asJson,
      "nonce" -> block.nonce.toLong.asJson,
      "transactions" -> block.transactions.map(_.asJson).asJson,
      "signature" -> Base58.encode(block.signature.bytes).asJson,
      "generator" -> Base58.encode(block.generator.bytes).asJson
    ).asJson
  }
}
