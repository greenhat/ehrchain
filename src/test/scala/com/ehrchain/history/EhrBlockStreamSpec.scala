package com.ehrchain.history

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers}
import scorex.core.consensus.History.HistoryComparisonResult
import scorex.core.{ModifierId, ModifierTypeId}

import scala.annotation.tailrec

class EhrBlockStreamSpec extends FlatSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  "generated history" should "have height" in {
      generateBlockStream(2).height shouldEqual 2
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
        for {
          (_, blockId) <- ids.headOption
        } yield blockId.mkString shouldEqual e.block.id.mkString
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
    val stream = generateBlockStream(2)
    stream.takeWhile(_.height == 2).toList shouldEqual stream.toList.takeWhile(_.height == 2)
  }

  it should "have drop equivalence with List" in {
    val stream = generateBlockStream(2)
    stream.drop(1).toList shouldEqual stream.toList.drop(1)
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
    val subStream = fullStream.drop(1)
    fullStream.compare(fullStream.syncInfo) shouldEqual HistoryComparisonResult.Equal
  }
}
