package ehr.demo

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.Instant.now

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import ehr.crypto.{AesCipher, EcdhDerivedKey}
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.record.{ByteArrayFileSource, FileHash, Record, RecordFileStorage}
import ehr.transaction.{EhrRecordTransactionCompanion, RecordTransaction}
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object ProviderATransactionGenerator {

  def recordTx(recordFileStorage: RecordFileStorage): Try[RecordTransaction] = {
    val recordFileContent = s"health record appended by provider A on ${now.toString}".getBytes
    val encryptedRecordFileStream = new ByteArrayOutputStream()
    for {
      _ <- AesCipher.encrypt(new ByteArrayInputStream(recordFileContent),
        encryptedRecordFileStream,
        EcdhDerivedKey.derivedKey(PatientTransactionGenerator.providerAKeyPair,
          PatientTransactionGenerator.patientKeyPair.publicKey))
      recordFileSource  = ByteArrayFileSource(encryptedRecordFileStream.toByteArray)
      fileHash <- FileHash.generate(recordFileSource)
      _ = recordFileStorage.put(fileHash, recordFileSource)
    } yield EhrRecordTransactionCompanion.generate(PatientTransactionGenerator.patientKeyPair.publicKey,
        PatientTransactionGenerator.providerAKeyPair, Record(Seq(fileHash)), now)
  }

  def behavior(viewHolderRef: ActorRef): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (ctx, msg) =>
      msg match {
        case NodeViewHolderCallback(view) =>
          val _ = ctx.system.scheduler.scheduleOnce(10 seconds,
            viewHolderRef,
            LocallyGeneratedTransaction[PublicKey25519Proposition, RecordTransaction](
              recordTx(view.history.storage.recordFileStorage).get))
          same
      }
    }
}
