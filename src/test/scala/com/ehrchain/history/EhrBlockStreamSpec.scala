package com.ehrchain.history

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers}
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
      stream.continuationIds(new EhrSyncInfo(e.block.id), 1).map ( ids => {
        require(ids.lengthCompare(1) == 0)
        for {
          (_, blockId) <- ids.headOption
        } yield blockId.mkString shouldEqual e.block.id.mkString
      }) should not be None
    )
  }
}
