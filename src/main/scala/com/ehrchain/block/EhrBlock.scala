package com.ehrchain.block

import com.ehrchain.core._
import com.ehrchain.serialization._
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionRecord}
import com.google.common.primitives.{Bytes, Ints, Longs}
import examples.commons.Nonce
import io.circe.Json
import io.circe.syntax._
import scorex.core.block.Block
import scorex.core.block.Block.{BlockId, Version}
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.transaction.state.{PrivateKey25519, PrivateKey25519Companion}
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.{Curve25519, PublicKey, Signature}

import scala.annotation.tailrec
import scala.util.{Random, Try}

final class EhrBlock(val parentId: BlockId,
                     val timestamp: TimeStamp,
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

  override def json: Json = Map(
    "id" -> Base58.encode(id).asJson,
    "parentId" -> Base58.encode(parentId).asJson,
    "timestamp" -> timestamp.toLong.asJson,
    "nonce" -> nonce.toLong.asJson,
    "transactions" -> transactions.map(_.json).asJson,
    "signature" -> Base58.encode(signature.bytes).asJson,
    "generator" -> Base58.encode(generator.bytes).asJson
  ).asJson

  override def toString: String = s"EhrBlock(${json.noSpaces}})"

  override def id: ModifierId =
    ModifierId @@ Blake2b256(parentId ++ serialize(timestamp) ++ generator.bytes)

  override def serializer: Serializer[EhrBlock] = EhrBlockSerializer

  lazy val validity: Boolean =
    timestamp > 0 &&
      transactions.nonEmpty &&
      signature.isValid(generator, EhrBlock.generateMessageToSign(parentId, timestamp, nonce, transactions, generator, difficulty)) &&
      powValidity

  lazy val powValidity: Boolean = {
    (difficulty == 0) || Blake2b256(bytes).startsWith(Array.fill[Byte](difficulty)(0))
  }
}

object EhrBlock {

  val MaxBlockSize: Int = 512 * 1024  //512K
  val ModifierType: ModifierTypeId = ModifierTypeId @@ 1.toByte

  def generateMessageToSign(parentId: BlockId,
                            timestamp: TimeStamp,
                            nonce: Nonce,
                            transactions: Seq[EhrTransaction],
                            generator: PublicKey25519Proposition,
                            difficulty: Int): Array[Byte] =
    Bytes.concat(
      parentId,
      serialize(timestamp),
      serialize(nonce),
      serialize(transactions),
      serialize(generator),
      Ints.toByteArray(difficulty),
    )

  @tailrec
  def generate(parentId: BlockId,
               timestamp: TimeStamp,
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
}

object EhrBlockSerializer extends Serializer[EhrBlock] {
  override def toBytes(obj: EhrBlock): Array[Byte] =
    Bytes.concat(
      obj.parentId,
      serialize(obj.timestamp),
      serialize(obj.nonce),
      serialize(obj.signature),
      serialize(obj.generator),
      Ints.toByteArray(obj.difficulty),
      serialize(obj.transactions)
    )

  override def parseBytes(bytes: Array[Byte]): Try[EhrBlock] = {
    require(bytes.length <= EhrBlock.MaxBlockSize)
    val parentIdEnd = Block.BlockIdLength
    val parentId = ModifierId @@ bytes.slice(0, parentIdEnd)
    val timestampEnd = parentIdEnd + 8
    val timestamp = TimeStamp @@ Longs.fromByteArray(bytes.slice(parentIdEnd, timestampEnd))
    val nonceEnd = timestampEnd + 8
    val nonce = Nonce @@ Longs.fromByteArray(bytes.slice(timestampEnd, nonceEnd))
    val signatureEnd = nonceEnd + Curve25519.SignatureLength
    val signature = Signature25519(Signature @@ bytes.slice(nonceEnd, signatureEnd))
    val generatorEnd = signatureEnd + Curve25519.KeyLength
    val generatorPK = PublicKey @@ bytes.slice(signatureEnd, generatorEnd)
    val generator = PublicKey25519Proposition(generatorPK)
    val difficultyEnd = generatorEnd + 4
    val difficulty = Ints.fromByteArray(bytes.slice(generatorEnd, difficultyEnd))
    for {
      transactions <- transactionsSerializer.parseBytes(bytes.slice(difficultyEnd, bytes.length))
    } yield new EhrBlock(parentId, timestamp, nonce, transactions, signature, generator, difficulty)
  }
}
