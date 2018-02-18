package com.ehrchain

import akka.actor.{ActorRef, Props}
import com.ehrchain.block.EhrBlock
import com.ehrchain.mining.EhrMiner.{MineBlock, StartMining, StopMining}
import com.ehrchain.transaction.EhrTransaction
import scorex.core.{LocalInterface, ModifierId}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class EhrLocalInterface(override val viewHolderRef: ActorRef,
                        minerRef: ActorRef)
  extends LocalInterface[PublicKey25519Proposition, EhrTransaction, EhrBlock] {

  //noinspection ActorMutableStateInspection
  private var isBlocked: Boolean = false

  override protected def onSuccessfulTransaction(tx: EhrTransaction): Unit = {}

  override protected def onFailedTransaction(tx: EhrTransaction): Unit = {}

  override protected def onStartingPersistentModifierApplication(pmod: EhrBlock): Unit = {}

  override protected def onSyntacticallySuccessfulModification(mod: EhrBlock): Unit = {}

  override protected def onSyntacticallyFailedModification(mod: EhrBlock): Unit = {}

  override protected def onSemanticallyFailedModification(mod: EhrBlock): Unit = {}

  override protected def onNewSurface(newSurface: Seq[ModifierId]): Unit = {}

  override protected def onRollbackFailed(): Unit = {
    log.error("rollback failed")
  }

  override protected def onSemanticallySuccessfulModification(mod: EhrBlock): Unit = {
    if (!isBlocked) {
          minerRef ! MineBlock
    }
  }

  override protected def onNoBetterNeighbour(): Unit = {
    minerRef ! StartMining
    isBlocked = false
  }

  override protected def onBetterNeighbourAppeared(): Unit = {
    minerRef ! StopMining
    isBlocked = true
  }
}

object EhrLocalInterface {

  def props(nodeViewHolderRef: ActorRef, minerRef: ActorRef) : Props =
    Props(new EhrLocalInterface(nodeViewHolderRef, minerRef))
}