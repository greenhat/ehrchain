package com.ehrchain.history

import com.ehrchain.EhrGenerators
import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{FlatSpec, Matchers}
import scorex.core.{ModifierId, ModifierTypeId}

import scala.annotation.tailrec

class EhrHistorySpec extends FlatSpec
  with PropertyChecks
  with GeneratorDrivenPropertyChecks
  with Matchers
  with EhrGenerators {

  @tailrec
  private def blockIdBelow(history: EhrHistory, below: Long, fromBlockId: ModifierId): Option[ModifierId] =
    history.modifierById(fromBlockId).map(_.parentId) match {
      case Some(blockId) if below > 0 => blockIdBelow(history, below - 1, blockId)
      case Some(blockId) => Some(blockId)
      case None => None
    }

  "generated history" should "have height" in {
      generateBlockStream(2).height shouldEqual 2
  }

  it should "have openSurfaceIds" in {
    val history = generateBlockStream(2)
    history.openSurfaceIds().nonEmpty shouldEqual true
  }

  it should "have continuationIds" in {
    val history = generateHistory(3)
    history.openSurfaceIds().map { blockId =>
      blockIdBelow(history, 1, blockId)
    }.headOption.flatten.flatMap { blockId =>
      history.continuationIds(new EhrSyncInfo(blockId), 1)
    } should not be None
  }

}
