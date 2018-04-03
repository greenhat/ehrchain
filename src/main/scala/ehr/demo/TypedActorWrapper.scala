package ehr.demo

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ActorRef, Props}
import ehr.TransactionMemPool
import ehr.core.NodeViewHolderCurrentView
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.history.BlockStream
import ehr.state.EhrMinimalState
import ehr.wallet.Wallet
import scorex.core.NodeViewHolder.ReceivableMessages.GetDataFromCurrentView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class TypedActorWrapper(viewHolderRef: ActorRef,
                        behavior: Behavior[NodeViewHolderCallback]) extends Actor {

  import TypedActorWrapper._

  private val typedActor =
    context.spawn[NodeViewHolderCallback](behavior, "typedActorTransactionGenerator")

  private val getRequiredData = GetDataFromCurrentView[
    BlockStream,
    EhrMinimalState,
    Wallet,
    TransactionMemPool,
    NodeViewHolderCallback]
    {  view: NodeViewHolderCurrentView =>
      NodeViewHolderCallback(view.vault)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    // todo extract Schedule message
    case Schedule(duration) =>
      val _ = context.system.scheduler.schedule(duration, duration, viewHolderRef, getRequiredData)
    case Call =>
      viewHolderRef ! getRequiredData
    case NodeViewHolderCallback(wallet) =>
      typedActor ! NodeViewHolderCallback(wallet)
  }
}

object TypedActorWrapper {

  def props(nodeViewHolder: ActorRef, behavior: Behavior[NodeViewHolderCallback]): Props = Props(new TypedActorWrapper(nodeViewHolder, behavior))

  final case object Call
  final case class Schedule(delay: FiniteDuration)

  final case class NodeViewHolderCallback(wallet: Wallet)
}


