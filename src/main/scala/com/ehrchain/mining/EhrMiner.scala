package com.ehrchain.mining

import akka.actor.{Actor, ActorRef}
import scorex.core.utils.ScorexLogging

class EhrMiner(viewHolderRef: ActorRef) extends Actor with ScorexLogging {
  import com.ehrchain.mining.EhrMiner._

  override def receive: Receive = stopped

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def mining: Receive = {
    case StopMining =>
      context.become(stopped)
    case MineBlock =>
      // todo pack txs from the mem pool into a block and "mine" it
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def stopped: Receive = {
    case StartMining =>
      context.become(mining)
      self ! MineBlock
  }
}

object EhrMiner extends App {

  case object StartMining

  case object StopMining

  case object MineBlock
}
