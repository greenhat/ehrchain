package com.ehrchain

import com.ehrchain.core.TimeStamp
import com.google.common.primitives.Longs
import scorex.core.serialization.Serializer

import scala.util.Try

package object serialization {

  implicit val timestampSerializer: Serializer[TimeStamp] = new Serializer[TimeStamp] {
    override def toBytes(obj: TimeStamp): Array[Byte] = Longs.toByteArray(obj)

    override def parseBytes(bytes: Array[Byte]): Try[TimeStamp] =
      Try {TimeStamp @@ Longs.fromByteArray(bytes) }
  }

  def serialize[T](t: T)(implicit serializer: Serializer[T]): Array[Byte] = serializer.toBytes(t)
}
