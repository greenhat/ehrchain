package ehr.state

import ehr.EhrGenerators
import ehr.contract.InMemoryContractStorage
import ehr.history.BlockStream
import ehr.record.{InMemoryRecordFileStorage, InMemoryRecordFileStorageMock}
import ehr.transaction.InMemoryRecordTransactionStorage
import org.scalatest._
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import scorex.core.VersionTag

import scala.util.{Success, Try}

class EhrMinimalStatePropSpec extends PropSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

}

class EhrMinimalStateFlatSpec extends FlatSpec
  with Matchers
  with EhrGenerators {

  "EhrMinimalState" should "process valid blocks" in {
    val initialState = EhrMinimalState(
      VersionTag @@ BlockStream.GenesisParentId,
      new InMemoryContractStorage(),
      InMemoryRecordFileStorageMock.storage,
      new InMemoryRecordTransactionStorage())
    validBlockstream.toList.foldRight(Try {initialState}) { case (element, state) =>
      state.flatMap(_.applyModifier(element.block))
    }.map(_ => true) shouldBe Success(true)
  }

  "EhrMinimalState" should "validate unauthorized tx" in {
    val initialState = EhrMinimalState(
      VersionTag @@ BlockStream.GenesisParentId,
      new InMemoryContractStorage(),
      new InMemoryRecordFileStorage(),
      new InMemoryRecordTransactionStorage())
    ehrRecordTransactionGen.sample.map { tx =>
      initialState.validate(tx).isSuccess
    } shouldEqual Some(false)
  }
}
