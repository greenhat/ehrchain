package com.ehrchain.crypto

import java.io.InputStream

import com.ehrchain.EhrGenerators
import com.sun.xml.internal.messaging.saaj.util.{ByteInputStream, ByteOutputStream}
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}

import scala.util.Success

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class AESCipherSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("encrypt/decrypt") {
    forAll(key25519PairGen, key25519PairGen) { (party1Keys, party2Keys) =>
      val senderKey = ECDHDerivedKey.derivedKey(party1Keys, party2Keys.publicKey)

      val originalContent = "mock file".getBytes
      val inputStream: InputStream = new ByteInputStream(originalContent, originalContent.length)
      val outputStream = new ByteOutputStream()

      AESCipher.encrypt(inputStream, outputStream, senderKey) shouldEqual Success()

      val encryptedInputStream: InputStream = new ByteInputStream(outputStream.getBytes,
        outputStream.getBytes.length)
      val decryptedOutputStream = new ByteOutputStream()
      val receiverKey = ECDHDerivedKey.derivedKey(party2Keys, party1Keys.publicKey)
      AESCipher.decrypt(encryptedInputStream, decryptedOutputStream, receiverKey) shouldEqual Success()

      decryptedOutputStream.getBytes shouldEqual outputStream.getBytes
    }
  }
}
