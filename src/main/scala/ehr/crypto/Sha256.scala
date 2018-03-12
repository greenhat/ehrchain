package ehr.crypto

import java.io.InputStream
import java.security.MessageDigest

import ehr.core.DigestSha256

import scala.util.Try

object Sha256 {

  @SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.While"))
  def digest(in: InputStream): Try[DigestSha256] = Try {
    val buffer = new Array[Byte](1024)
    val sha256 = MessageDigest.getInstance("SHA-256")
    var bytesRead = in.read(buffer)
    while (bytesRead != -1) {
      sha256.update(buffer, 0, bytesRead)
      bytesRead = in.read(buffer)
    }
    DigestSha256(sha256.digest)
  }
}
