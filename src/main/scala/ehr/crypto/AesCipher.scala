package ehr.crypto

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.security.SecureRandom

import ehr.core.{AesEncryptedData, KeyAes256}
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.{Cipher, CipherInputStream, CipherOutputStream}

import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.While"))
object AesCipher {

  private val cipherInstanceName = "AES/GCM/NoPadding"
  private val ivLengthByte = 12
  private val tagLengthBit = 128

  def encrypt(in: InputStream, out: OutputStream, key: KeyAes256): Try[Unit] = Try {
    val iv = new Array[Byte](ivLengthByte)
    new SecureRandom().nextBytes(iv)
    val cipher = Cipher.getInstance(cipherInstanceName)
    val parameterSpec = new GCMParameterSpec(tagLengthBit, iv)
    cipher.init(Cipher.ENCRYPT_MODE, key.secretKeySpec, parameterSpec)

    // write IV to the output stream first
    out.write(iv.length.toByte)
    out.write(iv)

    val cos = new CipherOutputStream(out, cipher)
    val data = new Array[Byte](1024)
    var read = in.read(data)
    while (read != -1) {
      cos.write(data, 0, read)
      read = in.read(data)
    }
    cos.flush()

    out.close()
    cos.close()
    in.close()
  }

  def decrypt(in: InputStream, out: OutputStream, key: KeyAes256): Try[Unit] = Try {
    // read IV length
    val ivLengthRead = in.read()
    require(ivLengthRead > 0)
    // read IV
    val iv = new Array[Byte](ivLengthRead)
    require(in.read(iv) == ivLengthRead)

    val cipher = Cipher.getInstance(cipherInstanceName)
    val parameterSpec = new GCMParameterSpec(tagLengthBit, iv)
    cipher.init(Cipher.DECRYPT_MODE, key.secretKeySpec, parameterSpec)

    val cis = new CipherInputStream(in, cipher)
    val data = new Array[Byte](1024)
    var read = cis.read(data)
    while(read != -1) {
      out.write(data, 0, read)
      read = cis.read(data)
    }
    out.close()
    cis.close()
    in.close()
  }

  def decryptInMemoryStream(in: InputStream, key: KeyAes256): Try[Array[Byte]] = {
    val out = new ByteArrayOutputStream()
    decrypt(in, out, key).map(_ => out.toByteArray)
  }

  def decryptInMemoryBytes(in: AesEncryptedData, key: KeyAes256): Try[Array[Byte]] = {
    val out = new ByteArrayOutputStream()
    decrypt(new ByteArrayInputStream(in.bytes), out, key).map(_ => out.toByteArray)
  }

  def encryptInMemory(in: Array[Byte], key: KeyAes256): Try[Array[Byte]] = {
    val out = new ByteArrayOutputStream()
    encrypt(new ByteArrayInputStream(in), out, key).map(_ => out.toByteArray)
  }

}
