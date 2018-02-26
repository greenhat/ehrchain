package com.ehrchain.state

import com.ehrchain.{EhrGenerators, EhrNodeViewHolder}
import com.ehrchain.contract.EhrInMemoryContractStorage
import com.ehrchain.history.EhrBlockStream
import com.ehrchain.transaction.{EhrContractTransaction, EhrRecordTransaction}
import org.scalatest.{Failed, Matchers, PropSpec, Succeeded}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import scorex.core.VersionTag

import scala.util.Success

class EhrMinimalStateSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  property("add contract, validate tx") {
    // fixme rewrite with block via applyModifier call
    val contractStorage = new EhrInMemoryContractStorage()
    val state = EhrMinimalState(VersionTag @@ EhrBlockStream.GenesisParentId, contractStorage)
    forAll(ehrTransactionPairGen) {
      case (contractTx: EhrContractTransaction) :: (recordTx: EhrRecordTransaction) :: Nil =>
        contractStorage.add(contractTx.contract).map { _ =>
          state.validate(recordTx) shouldBe Success()
        } shouldEqual Success(Succeeded)
      case _ => Failed(new Error("incorrect tx pair"))
    }
  }
}
