package com.ehrchain

import com.ehrchain.core.TimeStamp
import com.ehrchain.transaction.{EhrTransaction, EhrTransactionSerializer}
import com.google.common.primitives.{Bytes, Ints, Longs}
import examples.commons.Nonce
import scorex.core.serialization.Serializer

import scala.util.Try

package object serialization {

  implicit val timestampSerializer: Serializer[TimeStamp] = new Serializer[TimeStamp] {
    override def toBytes(obj: TimeStamp): Array[Byte] = Longs.toByteArray(obj)

    override def parseBytes(bytes: Array[Byte]): Try[TimeStamp] =
      Try { TimeStamp @@ Longs.fromByteArray(bytes) }
  }

  implicit val nonceSerializer: Serializer[Nonce] = new Serializer[Nonce] {
    override def toBytes(obj: Nonce): Array[Byte] = Longs.toByteArray(obj)

    override def parseBytes(bytes: Array[Byte]): Try[Nonce] =
      Try { Nonce @@ Longs.fromByteArray(bytes) }
  }

  implicit val transactionsSerializer: Serializer[Seq[EhrTransaction]] = new Serializer[Seq[EhrTransaction]] {
    override def toBytes(txs: Seq[EhrTransaction]): Array[Byte] =
      Bytes.concat(
        Ints.toByteArray(txs.length),
        txs.foldLeft(Array[Byte]()) { (a, b) =>
          Bytes.concat(Ints.toByteArray(b.bytes.length), b.bytes, a)
        }
      )

    @SuppressWarnings(Array("org.wartremover.warts.Recursion")) // transaction qty is limited
    override def parseBytes(bytes: Array[Byte]): Try[Seq[EhrTransaction]] = {
      val txsQtyEnd = 4
      val txsQty = Ints.fromByteArray(bytes.slice(0, txsQtyEnd))

      def loop(bytes: Array[Byte], txQty: Int): Try[Seq[EhrTransaction]] = txQty match {
        case 0 => Try { Nil }
        case txQtyLeft =>
          val txSizeEnd = 4
          val txSize = Ints.fromByteArray(bytes.slice(0, txSizeEnd))
          val txStart = txSizeEnd
          for {
            tx <- EhrTransactionSerializer.parseBytes(bytes.slice(txStart, txStart + txSize))
            txs <- loop(bytes.slice(txStart + txSize, bytes.length), txQtyLeft - 1)
          } yield Seq(tx) ++ txs
      }
      loop(bytes.slice(txsQtyEnd, bytes.length), txsQty)
    }
  }

  def serialize[T](t: T)(implicit serializer: Serializer[T]): Array[Byte] = serializer.toBytes(t)
}
