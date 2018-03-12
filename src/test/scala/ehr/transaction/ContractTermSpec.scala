package ehr.transaction

import java.time.Instant

import ehr.contract.{ContractTerm, Unlimited, ValidUntil}
import org.scalatest.{FlatSpec, Matchers}
import ehr.serialization._

import scala.util.Success

class ContractTermSpec extends FlatSpec
  with Matchers {

  "EhrContractTerm" should "Unlimited be serializable" in {
    deserializeFromBytes[ContractTerm](serializeToBytes(Unlimited)) shouldBe Success(Unlimited)
  }

  it should "ValidUntil be serializable" in {
    deserializeFromBytes[ContractTerm](serializeToBytes(ValidUntil(Instant.EPOCH))) shouldBe Success(ValidUntil(Instant.EPOCH))
  }
}
