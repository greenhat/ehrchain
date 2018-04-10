package ehr.mempool

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.stopped
import ehr.block.EhrBlock
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback

import scala.language.postfixOps

object PurgeTransactionMempool {

  def behavior(block: EhrBlock): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (_, msg) =>
      msg match {
        case NodeViewHolderCallback(view) =>
          val _ = view.pool.purge(block.transactions)
          stopped
      }
    }
}
