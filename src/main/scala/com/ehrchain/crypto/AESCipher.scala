package com.ehrchain.crypto

import java.io.{InputStream, OutputStream}

import scala.util.Try


object AESCipher {

  def encrypt(in: InputStream, out: OutputStream, key: Array[Byte]): Try[Unit] = ???

  def decrypt(in: InputStream, out: OutputStream, key: Array[Byte]): Try[Unit] = ???

}
