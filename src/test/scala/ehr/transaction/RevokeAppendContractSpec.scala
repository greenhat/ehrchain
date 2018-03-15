package ehr.transaction

import ehr.EhrGenerators
import ehr.contract.RevokeAppendContract
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

@SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
class RevokeAppendContractSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  "revoke append contract" should "be valid with startDate = timestamp" in {
    RevokeAppendContract(key25519PairGen.sample.get.publicKey,
      key25519PairGen.sample.get.publicKey,
      currentTimestamp,
      currentTimestamp).semanticValidity shouldEqual Success()
  }

  it should "be valid with startDate > timestamp" in {
    RevokeAppendContract(key25519PairGen.sample.get.publicKey,
      key25519PairGen.sample.get.publicKey,
      currentTimestamp,
      currentTimestamp.plusSeconds(10)).semanticValidity shouldEqual Success()
  }

  it should "be invalid with startDate < timestamp" in {
    RevokeAppendContract(key25519PairGen.sample.get.publicKey,
      key25519PairGen.sample.get.publicKey,
      currentTimestamp,
      currentTimestamp.minusSeconds(10)).semanticValidity.isFailure shouldBe true
  }
}
