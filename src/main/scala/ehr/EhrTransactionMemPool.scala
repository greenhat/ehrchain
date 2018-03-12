package ehr

import ehr.transaction.EhrTransaction
import io.iohk.iodb.ByteArrayWrapper
import scorex.core.ModifierId
import scorex.core.transaction.MemoryPool
import scorex.core.utils.ScorexLogging

import scala.collection.concurrent.TrieMap
import scala.util.{Success, Try}

final case class EhrTransactionMemPool(unconfirmed: TrieMap[ByteArrayWrapper, EhrTransaction])
  extends MemoryPool[EhrTransaction, EhrTransactionMemPool] with ScorexLogging {

  override type NVCT = this.type

  private def key(id: Array[Byte]): ByteArrayWrapper = ByteArrayWrapper(id)

  override def getById(id: ModifierId): Option[EhrTransaction] =
    unconfirmed.get(key(id))

  override def contains(id: ModifierId): Boolean = unconfirmed.contains(key(id))

  override def getAll(ids: Seq[ModifierId]): Seq[EhrTransaction] = ids.flatMap(getById(_).toList)

  override def put(tx: EhrTransaction): Try[EhrTransactionMemPool] = Success {
    val _ = unconfirmed.put(key(tx.id), tx)
    this
  }

  override def put(txs: Iterable[EhrTransaction]): Try[EhrTransactionMemPool] = Success(putWithoutCheck(txs))

  override def putWithoutCheck(txs: Iterable[EhrTransaction]): EhrTransactionMemPool = {
    txs.foreach(tx => unconfirmed.put(key(tx.id), tx))
    this
  }

  override def remove(tx: EhrTransaction): EhrTransactionMemPool = {
    val _ = unconfirmed.remove(key(tx.id))
    this
  }

  override def take(limit: Int): Iterable[EhrTransaction] =
    unconfirmed.values.toSeq.sortBy(_.timestamp).take(limit)

  override def filter(condition: (EhrTransaction) => Boolean): EhrTransactionMemPool = {
    val _ = unconfirmed.retain { (k, v) =>
      condition(v)
    }
    this
  }

  override def size: Int = unconfirmed.size
}


object EhrTransactionMemPool {
  lazy val emptyPool: EhrTransactionMemPool = EhrTransactionMemPool(TrieMap())
}

