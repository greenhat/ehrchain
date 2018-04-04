package ehr.demo

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.Instant.now

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import ehr.crypto.{AesCipher, EcdhDerivedKey}
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.record.{ByteArrayFileSource, FileHash, Record}
import ehr.transaction.{EhrRecordTransactionCompanion, RecordTransaction}
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object ProviderATransactionGenerator {

  def recordTx: RecordTransaction = {
    val recordFileContent = s"health record appended by provider A on ${now.toString}".getBytes
    val encryptedRecordFileStream = new ByteArrayOutputStream()
    AesCipher.encrypt(new ByteArrayInputStream(recordFileContent),
      encryptedRecordFileStream,
      EcdhDerivedKey.derivedKey(PatientTransactionGenerator.providerAKeyPair,
        PatientTransactionGenerator.patientKeyPair.publicKey))
    .flatMap { _ =>
      val recordFileSource = ByteArrayFileSource(encryptedRecordFileStream.toByteArray)
      FileHash.generate(recordFileSource)
    }.map{ fileHash =>
      EhrRecordTransactionCompanion.generate(PatientTransactionGenerator.patientKeyPair.publicKey,
      PatientTransactionGenerator.providerAKeyPair, Record(Seq(fileHash)), now)
    }.get
  }

  def behavior(viewHolderRef: ActorRef): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (ctx, msg) =>
      msg match {
        case NodeViewHolderCallback(_) =>
          val _ = ctx.system.scheduler.scheduleOnce(10 seconds,
            viewHolderRef,
            LocallyGeneratedTransaction[PublicKey25519Proposition, RecordTransaction](recordTx))
          same
      }
    }
}


