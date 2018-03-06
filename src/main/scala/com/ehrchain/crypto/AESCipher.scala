package com.ehrchain.crypto

import java.io.{InputStream, OutputStream}
import java.security.SecureRandom

import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, CipherInputStream, CipherOutputStream}

import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.Var", "org.wartremover.warts.While"))
object AESCipher {

  private val cipherInstanceName = "AES/GCM/NoPadding"
  private val ivLengthByte = 12
  private val tagLengthBit = 128

  def encrypt(in: InputStream, out: OutputStream, key: Array[Byte]): Try[Unit] = Try {
    require(key.length > 16, "key length must be longer than 16 byte")
    val iv = new Array[Byte](ivLengthByte)
    new SecureRandom().nextBytes(iv)
    val cipher = Cipher.getInstance(cipherInstanceName)
    val secretKeySpec = new SecretKeySpec(key, "AES")
    val parameterSpec = new GCMParameterSpec(tagLengthBit, iv)
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec)

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

  def decrypt(in: InputStream, out: OutputStream, key: Array[Byte]): Try[Unit] = Try {
    require(key.length > 16, "key length must be longer than 16 byte")
    // read IV length
    val ivLengthRead = in.read()
    require(ivLengthRead > 0)
    // read IV
    val iv = new Array[Byte](ivLengthRead)
    require(in.read(iv) == ivLengthRead)

    val cipher = Cipher.getInstance(cipherInstanceName)
    val secretKeySpec = new SecretKeySpec(key, "AES")
    val parameterSpec = new GCMParameterSpec(tagLengthBit, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec)

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

}
