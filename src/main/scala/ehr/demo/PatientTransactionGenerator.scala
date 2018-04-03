package ehr.demo

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.Behaviors.same
import ehr.demo.TypedActorWrapper.NodeViewHolderCallback
import ehr.transaction.ContractTransaction
import scorex.core.LocallyGeneratedModifiersMessages.ReceivableMessages.LocallyGeneratedTransaction
import scorex.core.transaction.box.proposition.PublicKey25519Proposition

object PatientTransactionGenerator {

  def behavior(viewHolderRef: ActorRef): Behavior[NodeViewHolderCallback] =
    Behaviors.immutable[NodeViewHolderCallback] { (ctx, msg) =>
      msg match {
        case NodeViewHolderCallback(wallet) =>
          viewHolderRef !
            LocallyGeneratedTransaction[PublicKey25519Proposition, ContractTransaction](appendTx)
          same
      }
    }

  def appendTx: ContractTransaction = ???

}


