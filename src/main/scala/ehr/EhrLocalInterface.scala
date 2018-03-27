package ehr

import akka.actor.{ActorRef, Props}
import akka.typed.Behavior
import ehr.block.EhrBlock
import ehr.mining.Miner.{MineBlock, StartMining, StopMining}
import ehr.record.RecordFileDownloaderSupervisor
import ehr.transaction.EhrTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{LocalInterface, ModifierId}
import akka.typed.scaladsl.adapter._
import ehr.record.RecordFileDownloaderSupervisor.DownloadMissingFiles

class EhrLocalInterface(override val viewHolderRef: ActorRef,
                        minerRef: ActorRef,
                        recordFileDownloader: Behavior[RecordFileDownloaderSupervisor.DownloadMissingFiles])
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

  override protected def onSemanticallySuccessfulModification(mod: EhrBlock): Unit = {
    context.spawn(recordFileDownloader, "RecordFileDownloaded") ! DownloadMissingFiles(mod)
  }

  override protected def onNoBetterNeighbour(): Unit = {
    minerRef ! StartMining
  }

  override protected def onBetterNeighbourAppeared(): Unit = {
    minerRef ! StopMining
  }
}

object EhrLocalInterface {

  def props(nodeViewHolderRef: ActorRef,
            minerRef: ActorRef,
            recordFileDownloader: Behavior[RecordFileDownloaderSupervisor.DownloadMissingFiles]) : Props =
    Props(new EhrLocalInterface(nodeViewHolderRef, minerRef, recordFileDownloader))
}
