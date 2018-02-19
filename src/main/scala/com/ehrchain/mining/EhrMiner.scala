package com.ehrchain.mining

import akka.actor.{Actor, ActorRef, Props}
import com.ehrchain.EhrTransactionMemPool
import com.ehrchain.history.EhrBlockStream
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.transaction.EhrTransaction
import com.ehrchain.wallet.EhrWallet
import scorex.core.NodeViewHolder.{CurrentView, GetDataFromCurrentView}
import scorex.core.utils.ScorexLogging
import com.ehrchain.core.NodeViewHolderCurrentView

class EhrMiner(viewHolderRef: ActorRef) extends Actor with ScorexLogging {
  import com.ehrchain.mining.EhrMiner._

  private val getRequiredData = GetDataFromCurrentView[
    EhrBlockStream,
    EhrMinimalState,
    EhrWallet,
    EhrTransactionMemPool,
    CreateBlock]
    {  view: NodeViewHolderCurrentView =>
      CreateBlock(view.vault, view.pool)
    }

  override def receive: Receive = stopped

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def mining: Receive = {
    case StopMining =>
      context.become(stopped)
    case MineBlock =>
      viewHolderRef ! getRequiredData
    case CreateBlock(wallet, pool) =>
      // todo make a block and send it to the view holder
//      viewHolderRef ! LocallyGeneratedModifier[EhrBlock](block)

  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def stopped: Receive = {
    case StartMining =>
      context.become(mining)
      self ! MineBlock
  }
}

object EhrMiner extends App {

  def props(nodeViewHolderRef: ActorRef): Props = Props(new EhrMiner(nodeViewHolderRef))

  case object StartMining

  case object StopMining

  case object MineBlock

  final case class CreateBlock(wallet: EhrWallet, memPool: EhrTransactionMemPool)
}
