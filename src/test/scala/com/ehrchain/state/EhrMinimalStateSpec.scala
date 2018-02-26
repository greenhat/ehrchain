package com.ehrchain.state

import com.ehrchain.EhrGenerators
import com.ehrchain.contract.EhrInMemoryContractStorage
import com.ehrchain.history.EhrBlockStream
import com.ehrchain.transaction.{EhrContractTransaction, EhrRecordTransaction}
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest._
import scorex.core.VersionTag

import scala.util.{Failure, Success, Try}

class EhrMinimalStatePropSpec extends PropSpec
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

class EhrMinimalStateFlatSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  "EhrMinimalState" should "process valid blocks" in {
    val initialState = EhrMinimalState(
      VersionTag @@ EhrBlockStream.GenesisParentId,
      new EhrInMemoryContractStorage())
    generateBlockStream(4).toList.foldRight(Try {initialState}) { case (element, state) =>
      state.flatMap(_.applyModifier(element.block).flatMap { newState =>
        if (element.block.transactions.forall {
          case _: EhrContractTransaction => true
          case recordTx: EhrRecordTransaction => newState.validate(recordTx).isSuccess}) {
          Success(newState)
        } else {
          Failure[EhrMinimalState](new Error("fail"))
        }
      })
    }.isSuccess shouldBe true
  }
}
