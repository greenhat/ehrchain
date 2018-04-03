package ehr.demo

import java.time.Instant.now

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import ehr.contract.{AppendContract, Unlimited}
import ehr.crypto.Curve25519KeyPair
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.transaction.ContractTransaction
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition
import scorex.core.transaction.state.PrivateKey25519Companion

object PatientTransactionGenerator {

  val patientKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("patient seed".getBytes)
  val providerAKeyPair: Curve25519KeyPair =
    PrivateKey25519Companion.generateKeys("provider A seed".getBytes)

  val appendTx: ContractTransaction =
    ContractTransaction.generate(patientKeyPair,
      AppendContract(patientKeyPair.publicKey, providerAKeyPair.publicKey, now, Unlimited),
      now)

  def behavior(viewHolderRef: ActorRef): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (ctx, msg) =>
      msg match {
        case NodeViewHolderCallback(wallet) =>
          viewHolderRef !
            LocallyGeneratedTransaction[PublicKey25519Proposition, ContractTransaction](appendTx)
          same
      }
    }
}


