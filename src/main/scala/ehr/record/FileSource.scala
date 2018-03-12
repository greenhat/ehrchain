package ehr.record

import java.io.{ByteArrayInputStream, InputStream}

trait FileSource {
  def inputStream: InputStream
}

@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
final case class ByteArrayFileSource(bytes: Array[Byte]) extends FileSource {
  override def inputStream: InputStream = new ByteArrayInputStream(bytes)
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
object FileSource {
  implicit def fromByteArray(bytes: Array[Byte]): FileSource = ByteArrayFileSource(bytes)
}
