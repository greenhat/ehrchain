package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import scorex.core.block.Block.BlockId
import scorex.core.{ModifierId, ModifierTypeId}
import scorex.core.consensus.History.ModifierIds
import scorex.core.consensus.SyncInfo
import scorex.core.serialization.Serializer

class EhrSyncInfo(blockId: ModifierId) extends SyncInfo {

  override type M = this.type

  override def startingPoints: ModifierIds = Seq((EhrBlock.ModifierType, blockId))

  override def serializer: Serializer[EhrSyncInfo.this.type] = ???
}
