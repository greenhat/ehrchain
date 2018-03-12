package ehr

import akka.actor.{ActorRef, Props}
import ehr.block.EhrBlock
import ehr.mining.Miner.{MineBlock, StartMining, StopMining}
import ehr.transaction.EhrTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{LocalInterface, ModifierId}

@SuppressWarnings(Array("org.wartremover.warts.Var"))
class EhrLocalInterface(override val viewHolderRef: ActorRef,
                        minerRef: ActorRef)
  extends LocalInterface[PublicKey25519Proposition, EhrTransaction, EhrBlock] {

  override protected def onSuccessfulTransaction(tx: EhrTransaction): Unit = {
    minerRef ! MineBlock
  }

  override protected def onFailedTransaction(tx: EhrTransaction): Unit = {}

  override protected def onStartingPersistentModifierApplication(pmod: EhrBlock): Unit = {}

  override protected def onSyntacticallySuccessfulModification(mod: EhrBlock): Unit = {}

  override protected def onSyntacticallyFailedModification(mod: EhrBlock): Unit = {}

  override protected def onSemanticallyFailedModification(mod: EhrBlock): Unit = {}

  override protected def onNewSurface(newSurface: Seq[ModifierId]): Unit = {}

  override protected def onRollbackFailed(): Unit = {
    log.error("rollback failed")
  }

  override protected def onSemanticallySuccessfulModification(mod: EhrBlock): Unit = {}

  override protected def onNoBetterNeighbour(): Unit = {
    minerRef ! StartMining
  }

  override protected def onBetterNeighbourAppeared(): Unit = {
    minerRef ! StopMining
  }
}

object EhrLocalInterface {

  def props(nodeViewHolderRef: ActorRef, minerRef: ActorRef) : Props =
    Props(new EhrLocalInterface(nodeViewHolderRef, minerRef))
}
