package com.ehrchain.history

import scorex.core.{ModifierId, ModifierTypeId}
import scorex.core.consensus.History.ModifierIds
import scorex.core.consensus.SyncInfo
import scorex.core.serialization.Serializer

class EhrSyncInfo extends SyncInfo {

  override type M = this.type

  override def startingPoints: ModifierIds = Seq[(ModifierTypeId, ModifierId)]()

  override def serializer: Serializer[EhrSyncInfo.this.type] = ???
}
