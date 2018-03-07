package com.ehrchain.record

import java.io.{ByteArrayInputStream, InputStream}

trait RecordFileSource {
  def inputStream: InputStream
}

@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
final case class ByteArrayRecordFileSource(bytes: Array[Byte]) extends RecordFileSource {
  override def inputStream: InputStream = new ByteArrayInputStream(bytes)
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
object RecordFileSource {
  implicit def fromByteArray(bytes: Array[Byte]): RecordFileSource = ByteArrayRecordFileSource(bytes)
}
