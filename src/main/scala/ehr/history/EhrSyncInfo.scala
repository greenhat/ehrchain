package ehr.history

import ehr.block.EhrBlock
import scorex.core.consensus.History.ModifierIds
import scorex.core.consensus.SyncInfo
import scorex.core.network.message.SyncInfoMessageSpec
import scorex.core.serialization.Serializer
import scorex.core.{ModifierId, ModifierTypeId}
import ehr.serialization._


@SerialVersionUID(0L)
final class EhrSyncInfo(blockId: Option[ModifierId]) extends SyncInfo {

  override type M = EhrSyncInfo

  override def startingPoints: ModifierIds =
    blockId.map(id => Seq((EhrBlock.ModifierType, id)))
      .getOrElse(Seq[(ModifierTypeId, ModifierId)]())

  override def serializer: Serializer[M] = byteSerializer[M]
}

object EhrSyncInfoMessageSpec
  extends SyncInfoMessageSpec[EhrSyncInfo](byteSerializer[EhrSyncInfo].parseBytes)
