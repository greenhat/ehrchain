package com.ehrchain.crypto

import java.io.ByteArrayInputStream
import java.security.MessageDigest

import com.ehrchain.EhrGenerators
import com.ehrchain.core.DigestSha256
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}

import scala.util.Success

class Sha256Spec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

    property("equivalence to non-streamed") {
      forAll(genBytes(37, 199 * 11)) { originalContent =>
        Sha256.sha256(new ByteArrayInputStream(originalContent)) shouldEqual
          Success(DigestSha256(MessageDigest.getInstance("SHA-256").digest(originalContent)))
      }
    }
}
