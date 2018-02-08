package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import com.ehrchain.mining.EhrMiningSettings
import scorex.core.ModifierId

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures", "org.wartremover.warts.Var"))
class EhrHistoryStorage(settings: EhrMiningSettings) {

  private val store: scala.collection.mutable.Map[ModifierId, EhrBlock] = scala.collection.mutable.Map()
  private val heightStore: scala.collection.mutable.Map[ModifierId, Long] = scala.collection.mutable.Map()
  private var bestBlockIdValue: ModifierId = settings.GenesisParentId

  // fixme always zero
  def height: Long = heightOf(bestBlockId).getOrElse(0L)

  def modifierById(blockId: ModifierId): Option[EhrBlock] = store.get(blockId)

  // todo cover with tests
  def update(b: EhrBlock): Unit = {
    store(b.id) = b
    heightStore(b.id) = parentHeight(b) + 1
    if (height == parentHeight(b)) bestBlockIdValue = b.id
  }

  def heightOf(blockId: ModifierId): Option[Long] = heightStore.get(blockId)

  def parentHeight(block: EhrBlock): Long = heightOf(block.parentId).getOrElse(0L)

  def bestBlockId: ModifierId = bestBlockIdValue

}
