package com.ehrchain.core

import scorex.crypto.encode.Base58

trait ByteArrayWrapper extends Serializable {

  val data: Array[Byte]

  def size: Int = data.length

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.IsInstanceOf"))
  override def equals(o: Any): Boolean =
    o.isInstanceOf[ByteArrayWrapper] &&
      java.util.Arrays.equals(data, o.asInstanceOf[ByteArrayWrapper].data)

  override def hashCode: Int = java.util.Arrays.hashCode(data)

  override def toString: String = Base58.encode(data)
}

@SerialVersionUID(0L)
final case class DigestSha256(data: Array[Byte]) extends ByteArrayWrapper {
  require(data.length == 32)
}
