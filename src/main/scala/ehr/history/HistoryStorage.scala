package ehr.history

import ehr.block.EhrBlock
import ehr.contract.{ContractStorage, InMemoryContractStorage}
import ehr.record.{InMemoryRecordFileStorage, RecordFileStorage}
import ehr.transaction.{InMemoryRecordTransactionStorage, RecordTransactionStorage}
import scorex.core.ModifierId

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures", "org.wartremover.warts.Var", "org.wartremover.warts.DefaultArguments"))
class HistoryStorage(val recordFileStorage: RecordFileStorage = new InMemoryRecordFileStorage()) {

  val contractStorage: ContractStorage = new InMemoryContractStorage()
  val recordTransactionStorage: RecordTransactionStorage = new InMemoryRecordTransactionStorage()

  private val store: scala.collection.mutable.Map[String, EhrBlock] = scala.collection.mutable.Map()
  private val heightStore: scala.collection.mutable.Map[String, Long] = scala.collection.mutable.Map()
  private var bestBlockIdValue: Option[ModifierId] = None

  def height: Long = bestBlockId.flatMap(heightOf).getOrElse(0L)

  def modifierById(blockId: ModifierId): Option[EhrBlock] = store.get(blockId.mkString)

  def append(b: EhrBlock): Unit = {
    store(b.id.mkString) = b
    heightStore(b.id.mkString) = parentHeight(b) + 1
    bestBlockIdValue = Some(b.id)
  }

  def heightOf(blockId: ModifierId): Option[Long] = heightStore.get(blockId.mkString)

  def parentHeight(block: EhrBlock): Long = heightOf(block.parentId).getOrElse(0L)

  def bestBlockId: Option[ModifierId] = bestBlockIdValue

}
