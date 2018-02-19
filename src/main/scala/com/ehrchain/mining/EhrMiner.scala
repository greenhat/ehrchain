package com.ehrchain.mining

import java.time.Instant

import akka.actor.{Actor, ActorRef, Props}
import com.ehrchain.EhrTransactionMemPool
import com.ehrchain.block.EhrBlock
import com.ehrchain.history.EhrBlockStream
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.transaction.EhrTransaction
import com.ehrchain.wallet.EhrWallet
import scorex.core.NodeViewHolder.{CurrentView, GetDataFromCurrentView}
import scorex.core.utils.ScorexLogging
import com.ehrchain.core.{NodeViewHolderCurrentView, TimeStamp}
import examples.trimchain.simulation.OneMinerSimulation.Height
import scorex.core.LocalInterface.LocallyGeneratedModifier

class EhrMiner(viewHolderRef: ActorRef) extends Actor with ScorexLogging {
  import com.ehrchain.mining.EhrMiner._

  private val getRequiredData = GetDataFromCurrentView[
    EhrBlockStream,
    EhrMinimalState,
    EhrWallet,
    EhrTransactionMemPool,
    CreateBlock]
    {  view: NodeViewHolderCurrentView =>
      CreateBlock(view.vault, view.pool, view.history.headOption.get.block)
    }

  override def receive: Receive = stopped

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def mining: Receive = {
    case StopMining =>
      context.become(stopped)
    case MineBlock =>
      viewHolderRef ! getRequiredData
    case CreateBlock(wallet, pool, bestBlock) =>
      generateBlock(wallet, pool, bestBlock) match {
        case Left(error) => log.error(s"error mining block: $error")
        case Right(block) => viewHolderRef ! LocallyGeneratedModifier[EhrBlock](block)
      }
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

  final case class CreateBlock(wallet: EhrWallet, memPool: EhrTransactionMemPool, bestBlock: EhrBlock)

  def generateBlock(wallet: EhrWallet, memPool: EhrTransactionMemPool, bestBlock: EhrBlock): Either[Throwable, EhrBlock] =
    memPool.take(10).filter(_.validity) match {
      case Nil => Left[Throwable, EhrBlock](new Exception("no valid transactions found"))
      case ref@ _ =>
        Right[Throwable, EhrBlock](EhrBlock.generate(
          bestBlock.id,
          TimeStamp @@ Instant.now.getEpochSecond,
          ref.toSeq,
          wallet.blockGeneratorKeyPair,
          // todo difficulty = blockchain.height / X ?
          0))
    }
}
