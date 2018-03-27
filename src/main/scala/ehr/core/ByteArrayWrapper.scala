package ehr.core

import javax.crypto.spec.SecretKeySpec
import scorex.crypto.encode.Base58

import scala.util.Try

trait ByteArrayWrapper extends Serializable {

  val bytes: Array[Byte]

  def size: Int = bytes.length

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.IsInstanceOf"))
  override def equals(o: Any): Boolean =
    o.isInstanceOf[ByteArrayWrapper] &&
      java.util.Arrays.equals(bytes, o.asInstanceOf[ByteArrayWrapper].bytes)

  override def hashCode: Int = java.util.Arrays.hashCode(bytes)

  override def toString: String = Base58.encode(bytes)
}

@SerialVersionUID(0L)
final case class DigestSha256(bytes: Array[Byte]) extends ByteArrayWrapper {
  require(bytes.length == 32, "SHA-256 digest should be 32 bytes")
}

object DigestSha256 {

  def rawUnsafe(bytes: Array[Byte]): Try[DigestSha256] = Try { DigestSha256(bytes) }
}

@SerialVersionUID(0L)
final case class KeyAes256(bytes: Array[Byte]) extends ByteArrayWrapper {
  require(bytes.length == 32, "AES-256 key should be 32 bytes")

  val secretKeySpec: SecretKeySpec = new SecretKeySpec(bytes, "AES")
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
object KeyAes256 {
  implicit def fromDigestSha256(digest: DigestSha256): KeyAes256 = KeyAes256(digest.bytes)
}

trait AesEncryptedData extends ByteArrayWrapper

@SerialVersionUID(0L)
final case class EncryptedRecordKeys(bytes: Array[Byte]) extends AesEncryptedData {
  require(bytes.length > 0, "must not be empty")
}

