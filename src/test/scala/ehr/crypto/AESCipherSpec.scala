package ehr.crypto

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class AESCipherSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("encrypt/decrypt") {
    forAll(key25519PairGen, key25519PairGen, genBytes(37, 199 * 11)) { (party1Keys, party2Keys, originalContent) =>
      val senderKey = ECDHDerivedKey.derivedKey(party1Keys, party2Keys.publicKey)

      val inputStream = new ByteArrayInputStream(originalContent)
      val outputStream = new ByteArrayOutputStream()

      AESCipher.encrypt(inputStream, outputStream, senderKey) shouldEqual Success()

      val encryptedInputStream = new ByteArrayInputStream(outputStream.toByteArray)
      val decryptedOutputStream = new ByteArrayOutputStream()
      val receiverKey = ECDHDerivedKey.derivedKey(party2Keys, party1Keys.publicKey)
      AESCipher.decrypt(encryptedInputStream, decryptedOutputStream, receiverKey) shouldEqual Success()

      decryptedOutputStream.toByteArray shouldEqual originalContent
    }
  }
}
