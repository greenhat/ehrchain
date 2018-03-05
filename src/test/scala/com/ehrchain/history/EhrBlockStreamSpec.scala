package com.ehrchain.history

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers}
import scorex.core.ModifierId
import scorex.core.consensus.History.HistoryComparisonResult
import scorex.crypto.hash.Blake2b256

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class EhrBlockStreamSpec extends FlatSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  "generated history" should "have height" in {
      generateBlockStream(2).headBlockHeight shouldEqual 2
  }

  it should "have toList in order" in {
    val stream = generateBlockStream(2)
    val list = stream.toList
    stream.headOption shouldEqual list.headOption
  }

  it should "have openSurfaceIds" in {
    val stream = generateBlockStream(2)
    stream.openSurfaceIds().nonEmpty shouldEqual true
  }

  it should "have continuationIds" in {
    val stream = generateBlockStream(2)
    stream.take(2)
      .toList
      .reverse
      .headOption
      .map(e =>
      stream.continuationIds(new EhrSyncInfo(Some(e.block.id)), 1).map ( ids => {
        require(ids.lengthCompare(1) == 0)
        ids.headOption.map(_._2.mkString) shouldEqual stream.headOption.map(_.block.id.mkString)
      }) should not be None
    ) should not be None
  }

  it should "have syncInfo" in {
    val stream = generateBlockStream(2)
    stream.syncInfo.startingPoints.headOption.map { case (_, blockId) =>
      stream.headOption.map(_.block.id shouldEqual blockId)
    } should not be None
  }

  it should "have headOption equivalence with List" in {
    val stream = generateBlockStream(2)
    stream.headOption.toList.headOption shouldEqual stream.toList.headOption
  }

  it should "have lastOption equivalence with List" in {
    val stream = generateBlockStream(2)
    stream.lastOption.toList.headOption shouldEqual stream.toList.lastOption
  }

  it should "have take equivalence with List" in {
    val stream = generateBlockStream(2)
    stream.take(1).toList shouldEqual stream.toList.take(1)
  }

  it should "have takeWhile equivalence with List" in {
    val stream = generateBlockStream(3)
    stream.takeWhile(_.blockHeight < 3).toList shouldEqual stream.toList.takeWhile(_.blockHeight < 3)
  }

  it should "have takeWhile headBlockHeight" in {
    val stream = generateBlockStream(2)
    val streamSlice = stream.takeWhile(_.blockHeight != 1)
    streamSlice.toList.length shouldEqual 1
    streamSlice.headBlockHeight shouldEqual 2
  }

  it should "have drop equivalence with List" in {
    val stream = generateBlockStream(2)
    stream.drop(1).toList shouldEqual stream.toList.drop(1)
  }

  it should "have find equivalence with List" in {
    val stream = generateBlockStream(2)
    stream.find(_.blockHeight == 1) shouldEqual stream.toList.find(_.blockHeight == 1)
  }

  it should "have compare Younger" in {
    val fullStream = generateBlockStream(3)
    val subStream = fullStream.drop(1)
    fullStream.compare(subStream.syncInfo) shouldEqual HistoryComparisonResult.Younger
  }

  it should "have compare Older" in {
    val fullStream = generateBlockStream(3)
    val subStream = fullStream.drop(1)
    subStream.compare(fullStream.syncInfo) shouldEqual HistoryComparisonResult.Older
  }

  it should "have compare the same" in {
    val fullStream = generateBlockStream(3)
    fullStream.compare(fullStream.syncInfo) shouldEqual HistoryComparisonResult.Equal
  }

  it should "append a block with existing parent" in {
    val stream = generateBlockStream(3)
    stream.headOption
      .map(e => generateBlock(e.block.id))
      .map(stream.append)
      .map(_.isSuccess) shouldBe Some(true)
  }

  it should "fail to append a block with an unknown parent" in {
    val stream = generateBlockStream(3)
    stream.headOption
      .map(e => generateBlock(ModifierId @@ Blake2b256("some id".getBytes)))
      .map(stream.append)
      .map(_.isFailure) shouldBe Some(true)
  }
}
