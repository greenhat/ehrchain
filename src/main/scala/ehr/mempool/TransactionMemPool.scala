package ehr.mempool

import ehr.transaction.EhrTransaction
import io.iohk.iodb.ByteArrayWrapper
import scorex.core.ModifierId
import scorex.core.transaction.MemoryPool
import scorex.core.utils.ScorexLogging

import scala.collection.concurrent.TrieMap
import scala.util.{Success, Try}

final case class TransactionMemPool(unconfirmed: TrieMap[ByteArrayWrapper, EhrTransaction])
  extends MemoryPool[EhrTransaction, TransactionMemPool] with ScorexLogging {

  override type NVCT = this.type

  private def key(id: Array[Byte]): ByteArrayWrapper = ByteArrayWrapper(id)

  override def getById(id: ModifierId): Option[EhrTransaction] =
    unconfirmed.get(key(id))

  override def contains(id: ModifierId): Boolean = unconfirmed.contains(key(id))

  override def getAll(ids: Seq[ModifierId]): Seq[EhrTransaction] = ids.flatMap(getById(_).toList)

  override def put(tx: EhrTransaction): Try[TransactionMemPool] = Success {
    val _ = unconfirmed.put(key(tx.id), tx)
    this
  }

  override def put(txs: Iterable[EhrTransaction]): Try[TransactionMemPool] = Success(putWithoutCheck(txs))

  override def putWithoutCheck(txs: Iterable[EhrTransaction]): TransactionMemPool = {
    txs.foreach(tx => unconfirmed.put(key(tx.id), tx))
    this
  }

  override def remove(tx: EhrTransaction): TransactionMemPool = {
    val _ = unconfirmed.remove(key(tx.id))
    this
  }

  def purge(txs: Seq[EhrTransaction]): Unit = ???

  override def take(limit: Int): Iterable[EhrTransaction] =
    unconfirmed.values.toSeq.sortBy(_.timestamp).take(limit)

  override def filter(condition: (EhrTransaction) => Boolean): TransactionMemPool = {
    val _ = unconfirmed.retain { (k, v) =>
      condition(v)
    }
    this
  }

  override def size: Int = unconfirmed.size
}


object TransactionMemPool {
  lazy val emptyPool: TransactionMemPool = TransactionMemPool(TrieMap())
}

