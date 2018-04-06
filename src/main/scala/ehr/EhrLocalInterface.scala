package ehr

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorRef, Props}
import ehr.block.EhrBlock
import ehr.demo.TypedActorWrapper
import ehr.demo.TypedActorWrapper.Call
import ehr.mempool.PurgeTransactionMempool
import ehr.mining.Miner.{MineBlock, StartMining, StopMining}
import ehr.record.{RecordFileDownloaderSupervisor, RecordFileStorage}
import ehr.transaction.{EhrTransaction, RecordTransaction, RecordTransactionFileValidator}
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.{LocalInterface, ModifierId}
import ehr.record.RecordFileDownloaderSupervisor.DownloadFiles

class EhrLocalInterface(override val viewHolderRef: ActorRef,
                        minerRef: ActorRef,
                        recordFileDownloader: Behavior[RecordFileDownloaderSupervisor.Command],
                        recordFileStorage: RecordFileStorage)
  extends LocalInterface[PublicKey25519Proposition, EhrTransaction, EhrBlock] {

  override protected def onSuccessfulTransaction(tx: EhrTransaction): Unit = {
    minerRef ! MineBlock
    // todo request missing files (if any)
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

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  override protected def onSemanticallySuccessfulModification(mod: EhrBlock): Unit = {
    context.spawn(recordFileDownloader, "RecordFileDownloaderSupervisor") !
      DownloadFiles(
        new RecordTransactionFileValidator(recordFileStorage)
          .findMissingFiles(mod.transactions.collect { case recTx: RecordTransaction => recTx })
      )
    context.actorOf(TypedActorWrapper.props(viewHolderRef,
      PurgeTransactionMempool.behavior(mod))) ! Call
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
            recordFileDownloader: Behavior[RecordFileDownloaderSupervisor.Command],
            recordFileStorage: RecordFileStorage) : Props =
    Props(new EhrLocalInterface(nodeViewHolderRef, minerRef, recordFileDownloader,
      recordFileStorage))
}
