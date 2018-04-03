package ehr.demo

import java.time.Instant.now

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import ehr.contract.{AppendContract, ReadContract, RecordKeys, Unlimited}
import ehr.crypto.Curve25519KeyPair
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.transaction.ContractTransaction
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519Companion

@SuppressWarnings(Array("org.wartremover.warts.TryPartial"))
object PatientTransactionGenerator {

  val patientKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("patient seed".getBytes)
  val providerAKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("provider A seed".getBytes)
  val providerBKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("provider B seed".getBytes)

  val appendTx: ContractTransaction =
    ContractTransaction.generate(patientKeyPair,
      AppendContract(patientKeyPair.publicKey, providerAKeyPair.publicKey, now, Unlimited),
      now)

  val readTx: ContractTransaction =
    ContractTransaction.generate(patientKeyPair,
      ReadContract.generate(patientKeyPair,
        providerBKeyPair.publicKey,
        now,
        RecordKeys.build(patientKeyPair, Seq(providerAKeyPair.publicKey))).get,
      now
    )

  def behavior(viewHolderRef: ActorRef): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (_, msg) =>
      msg match {
        case NodeViewHolderCallback(_) =>
          viewHolderRef !
            LocallyGeneratedTransaction[PublicKey25519Proposition, ContractTransaction](appendTx)
          viewHolderRef !
            LocallyGeneratedTransaction[PublicKey25519Proposition, ContractTransaction](readTx)
          same
      }
    }
}


