package ehr.mempool

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.stopped
import ehr.block.EhrBlock
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.transaction.EhrTransaction

import scala.language.postfixOps

object PurgeTransactionMempool {

  def behavior(block: EhrBlock): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (ctx, msg) =>
      msg match {
        case NodeViewHolderCallback(view) =>
          purge(view.pool, block.transactions)
          stopped
      }
    }

  def purge(pool: TransactionMemPool, txs: Seq[EhrTransaction]): Unit = ???
}


