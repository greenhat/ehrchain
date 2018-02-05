package com.ehrchain.block

import com.ehrchain.core._
import com.ehrchain.serialization._
import com.ehrchain.transaction.EhrTransaction
import io.circe.Json
import io.circe.syntax._
import scorex.core.block.Block
import scorex.core.block.Block.{BlockId, Version}
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.proof.Signature25519
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256

class EhrBlock(
              val parentId: BlockId,
              val timestamp: TimeStamp,
              val nonce: Long,
              val transactions: Seq[EhrTransaction],
              val signature: Signature25519,
              val generator: PublicKey25519Proposition
              ) extends Block[PublicKey25519Proposition, EhrTransaction] {

  override type M = EhrBlock

  override lazy val modifierTypeId: ModifierTypeId = ModifierTypeId @@ 200.toByte

  override def version: Version = 1: Byte

  override def json: Json = Map(
    "id" -> Base58.encode(id).asJson
    // fixme add the rest
  ).asJson

  override def id: ModifierId =
    ModifierId @@ Blake2b256(parentId ++ serialize(timestamp) ++ generator.bytes)

  override def serializer: Serializer[EhrBlock] = ???
}
