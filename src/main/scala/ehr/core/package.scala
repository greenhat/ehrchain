package ehr

import ehr.history.EhrBlockStream
import ehr.state.EhrMinimalState
import ehr.wallet.EhrWallet
import scorex.core.NodeViewHolder.CurrentView

import scala.util.{Failure, Success, Try}

package object core {

  type NodeViewHolderCurrentView =
    CurrentView[EhrBlockStream, EhrMinimalState, EhrWallet, EhrTransactionMemPool]

  implicit class OptionOps[A](opt: Option[A]) {
    def toTry(msg: String): Try[A] = {
      opt
        .map(Success(_))
        .getOrElse(Failure[A](new NoSuchElementException(msg)))
    }
  }
}
