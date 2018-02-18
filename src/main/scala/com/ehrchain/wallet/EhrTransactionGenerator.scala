package com.ehrchain.wallet

import akka.actor.{Actor, ActorRef, Props}
import com.ehrchain.EhrTransactionMemPool
import com.ehrchain.history.EhrBlockStream
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.transaction.EhrTransaction
import scorex.core.NodeViewHolder.{CurrentView, GetDataFromCurrentView}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

class EhrTransactionGenerator(viewHolderRef: ActorRef) extends Actor {

  import EhrTransactionGenerator._

  type CurrentViewType = CurrentView[EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool]

  private val getRequiredData = GetDataFromCurrentView[
    EhrBlockStream,
    EhrMinimalState,
    EhrWallet,
    EhrTransactionMemPool,
    GenerateTransaction]
    {  view: CurrentViewType =>
    EhrTransactionGenerator.GenerateTransaction(view.vault)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case StartGeneration(duration) =>
      val _ = context.system.scheduler.schedule(duration, duration, viewHolderRef, getRequiredData)
  }

}

object EhrTransactionGenerator {

  def props(nodeViewHolder: ActorRef): Props = Props(new EhrTransactionGenerator(nodeViewHolder))

  final case class StartGeneration(delay: FiniteDuration)

  final case class GenerateTransaction(wallet: EhrWallet)
}
