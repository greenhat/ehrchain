package com.ehrchain

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.time.Instant

import com.ehrchain.core.TimeStamp
import com.ehrchain.transaction.{EhrRecordTransactionSerializer, EhrTransaction}
import com.google.common.primitives.{Bytes, Ints, Longs}
import examples.commons.Nonce
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519PropositionSerializer
import scorex.core.transaction.proof.Signature25519Serializer

import scala.util.Try

package object serialization {

  implicit val timestampSerializer: Serializer[TimeStamp] = new Serializer[TimeStamp] {
    override def toBytes(obj: TimeStamp): Array[Byte] = Longs.toByteArray(obj)

    override def parseBytes(bytes: Array[Byte]): Try[TimeStamp] =
      Try { TimeStamp @@ Longs.fromByteArray(bytes) }
  }

  implicit val instantSerializer: Serializer[Instant] = new Serializer[Instant] {
    override def toBytes(obj: Instant): Array[Byte] = Longs.toByteArray(obj.toEpochMilli)

    override def parseBytes(bytes: Array[Byte]): Try[Instant] =
      Try { Instant.ofEpochMilli(Longs.fromByteArray(bytes)) }
  }

  implicit val nonceSerializer: Serializer[Nonce] = new Serializer[Nonce] {
    override def toBytes(obj: Nonce): Array[Byte] = Longs.toByteArray(obj)

    override def parseBytes(bytes: Array[Byte]): Try[Nonce] =
      Try { Nonce @@ Longs.fromByteArray(bytes) }
  }

  // todo use generic EhrTransaction serializer
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
            tx <- EhrRecordTransactionSerializer.parseBytes(bytes.slice(txStart, txStart + txSize))
            txs <- loop(bytes.slice(txStart + txSize, bytes.length), txQtyLeft - 1)
          } yield txs ++ Seq(tx)
      }
      loop(bytes.slice(txsQtyEnd, bytes.length), txsQty)
    }
  }

  implicit val signature25519Serializer: Signature25519Serializer.type = Signature25519Serializer
  implicit val publicKey25519PropositionSerializer: PublicKey25519PropositionSerializer.type =
    PublicKey25519PropositionSerializer

  def serialize[T](t: T)(implicit serializer: Serializer[T]): Array[Byte] = serializer.toBytes(t)

  def serializeToBytes[T <: Serializable](t: T): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(t)
    oos.close()
    stream.toByteArray
  }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def deserializeFromBytes[T <: Serializable](bytes: Array[Byte]): Try[T] = Try {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject.asInstanceOf[T]
    ois.close()
    value
  }

  def byteSerializer[T <: Serializable]: Serializer[T] = {
    class ConcreteSerializer extends Serializer[T] {
      override def toBytes(obj: T): Array[Byte] = serializeToBytes(obj)
      override def parseBytes(bytes: Array[Byte]): Try[T] = deserializeFromBytes[T](bytes)
    }
    new ConcreteSerializer
  }
}
