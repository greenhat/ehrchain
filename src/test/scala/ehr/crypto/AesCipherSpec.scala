package ehr.crypto

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import ehr.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class AesCipherSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("encrypt/decrypt") {
    forAll(key25519PairGen, key25519PairGen, genBytes(37, 199 * 11)) { (party1Keys, party2Keys, originalContent) =>
      val senderKey = EcdhDerivedKey.derivedKey(party1Keys, party2Keys.publicKey)

      val inputStream = new ByteArrayInputStream(originalContent)
      val outputStream = new ByteArrayOutputStream()

      AesCipher.encrypt(inputStream, outputStream, senderKey) shouldEqual Success()

      val encryptedInputStream = new ByteArrayInputStream(outputStream.toByteArray)
      val decryptedOutputStream = new ByteArrayOutputStream()
      val receiverKey = EcdhDerivedKey.derivedKey(party2Keys, party1Keys.publicKey)
      AesCipher.decrypt(encryptedInputStream, decryptedOutputStream, receiverKey) shouldEqual Success()

      decryptedOutputStream.toByteArray shouldEqual originalContent
    }
  }
}
