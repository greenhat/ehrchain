package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import scorex.core.ModifierId

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
class EhrHistoryStorage {

  private val store: scala.collection.mutable.Map[ModifierId, EhrBlock] = scala.collection.mutable.Map()

  def modifierById(blockId: ModifierId): Option[EhrBlock] = store.get(blockId)

  def update(b: EhrBlock): Unit = store(b.id) = b
}
