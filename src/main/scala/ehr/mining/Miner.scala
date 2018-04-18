package ehr.mining

import java.time.Instant

import akka.actor.{Actor, ActorRef, Props}
import ehr.block.EhrBlock
import ehr.core.NodeViewHolderCurrentView
import ehr.history.BlockStream
import ehr.mempool.TransactionMemPool
import ehr.mining.Miner.{CreateBlock, MineBlock, StartMining, StopMining}
import ehr.state.EhrMinimalState
import ehr.wallet.Wallet
import scorex.core.NodeViewHolder.ReceivableMessages.{GetDataFromCurrentView, LocallyGeneratedModifier}
import scorex.core.block.Block.BlockId
import scorex.core.utils.ScorexLogging

class Miner(viewHolderRef: ActorRef) extends Actor with ScorexLogging {

  private val getRequiredData = GetDataFromCurrentView[
    BlockStream,
    EhrMinimalState,
    Wallet,
    TransactionMemPool,
    CreateBlock]
    {  view: NodeViewHolderCurrentView =>
      CreateBlock(
        view.vault,
        view.pool,
        view.history.headOption.map(_.block.id).getOrElse(BlockStream.GenesisParentId))
    }

  override def receive: Receive = mining

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def mining: Receive = {
    case StopMining =>
      context.become(stopped)
    case MineBlock =>
      viewHolderRef ! getRequiredData
    case CreateBlock(wallet, pool, bestBlockId) =>
      Miner.generateBlock(wallet, pool, bestBlockId) match {
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

object Miner extends App {

  def props(nodeViewHolderRef: ActorRef): Props = Props(new Miner(nodeViewHolderRef))

  case object StartMining

  case object StopMining

  case object MineBlock

  final case class CreateBlock(wallet: Wallet, memPool: TransactionMemPool, bestBlockId: BlockId)

  def generateBlock(wallet: Wallet,
                    memPool: TransactionMemPool,
                    bestBlockId: BlockId): Either[Throwable, EhrBlock] =
    memPool.take(10).filter(_.semanticValidity) match {
      case Nil => Left[Throwable, EhrBlock](new Exception("no valid transactions found"))
      case ref@ _ =>
        Right[Throwable, EhrBlock](EhrBlock.generate(
          bestBlockId,
          Instant.now,
          ref.toSeq,
          wallet.blockGeneratorKeyPair,
          1))
    }
}
