package ehr

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{Actor, ActorRef, Props}
import ehr.block.EhrBlock
import ehr.mining.Miner.{MineBlock, StartMining, StopMining}
import ehr.record.RecordFileDownloaderSupervisor.DownloadFiles
import ehr.record.{RecordFileDownloaderSupervisor, RecordFileStorage}
import ehr.transaction.{RecordTransaction, RecordTransactionFileValidator}
import scorex.core.network.NodeViewSynchronizer.Events.{BetterNeighbourAppeared, NoBetterNeighbour, NodeViewSynchronizerEvent}
import scorex.core.network.NodeViewSynchronizer.ReceivableMessages._
import scorex.core.utils.ScorexLogging

@SuppressWarnings(Array("org.wartremover.warts.Any", "org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Nothing", "org.wartremover.warts.ToString"))
class EhrLocalInterface(viewHolderRef: ActorRef,
                        minerRef: ActorRef,
                        recordFileDownloader: Behavior[RecordFileDownloaderSupervisor.Command],
                        recordFileStorage: RecordFileStorage)
  extends Actor with ScorexLogging {

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[NodeViewHolderEvent])
    context.system.eventStream.subscribe(self, classOf[NodeViewSynchronizerEvent])
  }

  private def downloadMissingFiles(txs: Seq[RecordTransaction]): Unit =
    context.spawn(recordFileDownloader, "RecordFileDownloaderSupervisor") !
      DownloadFiles(
        new RecordTransactionFileValidator(recordFileStorage)
          .findMissingFiles(txs))

  override def receive: Receive = {
    case RollbackFailed => log.error("rollback failed")

    case FailedTransaction(tx, error) =>
      log.error(s"transaction ${tx.toString} failed: ${error.getLocalizedMessage}")

    case SuccessfulTransaction(tx) =>
      // todo restore after fix in Scorex ([P] -> [+P])
//      tx match {
//        case recordTx: RecordTransaction => downloadMissingFiles(Seq(recordTx))
//      }
      minerRef ! MineBlock

    case SemanticallySuccessfulModifier(mod: EhrBlock) =>
      downloadMissingFiles(mod.transactions.collect { case recTx: RecordTransaction => recTx })

    case NoBetterNeighbour =>
      minerRef ! StartMining

    case BetterNeighbourAppeared =>
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
