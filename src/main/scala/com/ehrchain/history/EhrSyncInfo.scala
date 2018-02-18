package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.core.consensus.History.ModifierIds
import scorex.core.consensus.SyncInfo
import scorex.core.network.message.SyncInfoMessageSpec
import scorex.core.serialization.Serializer

import scala.util.Try

final class EhrSyncInfo(blockId: Option[ModifierId]) extends SyncInfo {

  override type M = EhrSyncInfo

  override def startingPoints: ModifierIds =
    blockId.map(id => Seq((EhrBlock.ModifierType, id))).getOrElse(Seq[(ModifierTypeId, ModifierId)]())

  override def serializer: Serializer[EhrSyncInfo] = EhrSyncInfoSerializer
}

object EhrSyncInfoSerializer extends Serializer[EhrSyncInfo] {
  override def toBytes(obj: EhrSyncInfo): Array[Byte] = ???

  override def parseBytes(bytes: Array[Byte]): Try[EhrSyncInfo] = ???
}

object EhrSyncInfoMessageSpec extends SyncInfoMessageSpec[EhrSyncInfo](EhrSyncInfoSerializer.parseBytes)
