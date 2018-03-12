package com.ehrchain

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.time.Instant

import com.google.common.primitives.Longs
import examples.commons.Nonce
import scorex.core.serialization.Serializer
import scorex.core.transaction.box.proposition.PublicKey25519PropositionSerializer
import scorex.core.transaction.proof.Signature25519Serializer

import scala.util.Try

package object serialization {

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
