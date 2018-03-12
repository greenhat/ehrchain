package ehr.transaction

import java.time.Instant

import ehr.contract.{EhrContractTerm, Unlimited, ValidUntil}
import org.scalatest.{FlatSpec, Matchers}
import ehr.serialization._

import scala.util.Success

class EhrContractTermSpec extends FlatSpec
  with Matchers {

  "EhrContractTerm" should "Unlimited be serializable" in {
    deserializeFromBytes[EhrContractTerm](serializeToBytes(Unlimited)) shouldBe Success(Unlimited)
  }

  it should "ValidUntil be serializable" in {
    deserializeFromBytes[EhrContractTerm](serializeToBytes(ValidUntil(Instant.EPOCH))) shouldBe Success(ValidUntil(Instant.EPOCH))
  }
}
