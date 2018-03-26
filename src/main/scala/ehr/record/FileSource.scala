package ehr.record

import java.io.{ByteArrayInputStream, InputStream}

import ehr.core.DigestSha256
import ehr.crypto.Sha256

import scala.util.Try

trait FileSource {
  def inputStream: InputStream

  def hash: Try[DigestSha256] = Sha256.digest(inputStream)
}

@SuppressWarnings(Array("org.wartremover.warts.ArrayEquals"))
final case class ByteArrayFileSource(bytes: Array[Byte]) extends FileSource {
  override def inputStream: InputStream = new ByteArrayInputStream(bytes)
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
object FileSource {
  implicit def fromByteArray(bytes: Array[Byte]): FileSource = ByteArrayFileSource(bytes)
}
