package com.ehrchain.wallet

import java.time.Instant

import akka.actor.{Actor, ActorRef, Props}
import com.ehrchain.EhrTransactionMemPool
import com.ehrchain.core.{NodeViewHolderCurrentView, RecordType, TimeStamp}
import com.ehrchain.history.EhrBlockStream
import com.ehrchain.state.EhrMinimalState
import com.ehrchain.transaction.{EhrRecordTransaction, EhrRecordTransactionCompanion}
import scorex.core.LocalInterface.LocallyGeneratedTransaction
import scorex.core.NodeViewHolder.GetDataFromCurrentView
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class EhrTransactionGenerator(viewHolderRef: ActorRef) extends Actor {

  import EhrTransactionGenerator._

  private val getRequiredData = GetDataFromCurrentView[
    EhrBlockStream,
    EhrMinimalState,
    EhrWallet,
    EhrTransactionMemPool,
    GenerateTransaction]
    {  view: NodeViewHolderCurrentView =>
      EhrTransactionGenerator.GenerateTransaction(view.vault)
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def receive: Receive = {
    case StartGeneration(duration) =>
      val _ = context.system.scheduler.schedule(duration, duration, viewHolderRef, getRequiredData)

    case GenerateTransaction(wallet) =>
      viewHolderRef ! LocallyGeneratedTransaction[PublicKey25519Proposition, EhrRecordTransaction](generateTx(wallet))
  }
}

object EhrTransactionGenerator {

  def props(nodeViewHolder: ActorRef): Props = Props(new EhrTransactionGenerator(nodeViewHolder))

  final case class StartGeneration(delay: FiniteDuration)

  final case class GenerateTransaction(wallet: EhrWallet)

  def generateTx(wallet: EhrWallet): EhrRecordTransaction =
    EhrRecordTransactionCompanion.generate(
      wallet.patientPK,
      wallet.providerKeyPair,
      RecordType @@ Array.fill[Byte](10)(0),
      TimeStamp @@ Instant.now.getEpochSecond)
}
