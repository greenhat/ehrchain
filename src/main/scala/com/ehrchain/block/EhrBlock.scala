package com.ehrchain.block

import com.ehrchain.core._
import com.ehrchain.serialization._
import com.ehrchain.transaction.EhrTransaction
import com.google.common.primitives.Bytes
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

import scala.util.Try

final class EhrBlock(
              val parentId: BlockId,
              val timestamp: TimeStamp,
              val nonce: Nonce,
              val transactions: Seq[EhrTransaction],
              val signature: Signature25519,
              val generator: PublicKey25519Proposition
              ) extends Block[PublicKey25519Proposition, EhrTransaction] {

  override type M = EhrBlock

  override lazy val modifierTypeId: ModifierTypeId = ModifierTypeId @@ 200.toByte

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

  // todo make TimeStamp unable to hold incorrect values
  lazy val validity: Try[Boolean] = Try {
    require(timestamp > 0)
    require(transactions.nonEmpty)
    signature.isValid(generator,
      EhrBlock.generateMessageToSign(parentId, timestamp, nonce, transactions, generator))
  }
}

object EhrBlock {

  def generateMessageToSign(parentId: BlockId,
                            timestamp: TimeStamp,
                            nonce: Nonce,
                            transactions: Seq[EhrTransaction],
                            generator: PublicKey25519Proposition): Array[Byte] =
    Bytes.concat(
      parentId,
      serialize(timestamp),
      serialize(nonce),
      serialize(transactions),
      serialize(generator)
    )
}

object EhrBlockCompanion {

  // todo move to test (generators)?
  def generate(parentId: BlockId,
               timestamp: TimeStamp,
               nonce: Nonce,
               transactions: Seq[EhrTransaction],
               generatorKeys: (PrivateKey25519, PublicKey25519Proposition)): EhrBlock = {
    val generatorSK = generatorKeys._1
    val generatorPK = generatorKeys._2
    val msgToSign = EhrBlock.generateMessageToSign(parentId, timestamp, nonce, transactions, generatorPK)
    val signature = PrivateKey25519Companion.sign(generatorSK, msgToSign)
    new EhrBlock(parentId, timestamp, nonce, transactions, signature, generatorPK)
  }
}

// todo test
object EhrBlockSerializer extends Serializer[EhrBlock] {
  override def toBytes(obj: EhrBlock): Array[Version] = {
    Bytes.concat(
      obj.parentId,
      serialize(obj.timestamp),
      serialize(obj.nonce),
      serialize(obj.transactions),
      serialize(obj.signature),
      serialize(obj.generator)
    )
  }

  // todo implement
  override def parseBytes(bytes: Array[Version]): Try[EhrBlock] = ???
}