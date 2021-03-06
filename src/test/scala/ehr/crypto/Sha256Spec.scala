package ehr.crypto

import java.io.ByteArrayInputStream
import java.security.MessageDigest

import ehr.EhrGenerators
import ehr.core.DigestSha256
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.util.Success

class Sha256Spec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

    property("equivalence to non-streamed") {
      forAll(genBytes(37, 199 * 11)) { originalContent =>
        Sha256.digest(new ByteArrayInputStream(originalContent)) shouldEqual
          Success(DigestSha256(MessageDigest.getInstance("SHA-256").digest(originalContent)))
      }
    }
}
