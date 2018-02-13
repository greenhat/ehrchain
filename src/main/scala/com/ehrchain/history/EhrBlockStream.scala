package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import scorex.core.{ModifierId, ModifierTypeId, NodeViewModifier}
import scorex.core.consensus.{History, ModifierSemanticValidity}
import scorex.core.consensus.History.{HistoryComparisonResult, ModifierIds, ProgressInfo}
import scorex.core.utils.ScorexLogging
import scorex.crypto.encode.Base58

import scala.annotation.tailrec
import scala.util.control.TailCalls.{TailRec, done, tailcall}
import scala.util.{Failure, Try}

trait EhrBlockStream extends History[EhrBlock, EhrSyncInfo, EhrBlockStream]
  with ScorexLogging {

  import EhrBlockStream._

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  override type NVCT = this.type

  implicit def storage: EhrHistoryStorage = ???

  def headBlockHeight: Long = headOption.map(_.blockHeight).getOrElse(0L)

  import EhrBlockStream._

  override def isEmpty: Boolean = headBlockHeight == 0

  override def modifierById(modifierId: ModifierId): Option[EhrBlock] = storage.modifierById(modifierId)

  override def append(block: EhrBlock): Try[(EhrBlockStream, History.ProgressInfo[EhrBlock])] = {
    log.debug(s"Trying to append block ${Base58.encode(block.id)} to history")
    if (block.validity) {
      storage.append(block)
      Try {
        (cons(EhrBlockStreamElement(block, storage.height), this),
          ProgressInfo(branchPoint = None,
            toRemove = Seq[EhrBlock](),
            toApply = Some(block),
            toDownload = Seq[(ModifierTypeId, ModifierId)]())
        )
      }
    } else Failure[(EhrBlockStream, History.ProgressInfo[EhrBlock])](new Exception("block is not valid"))
  }

  override def reportSemanticValidity(modifier: EhrBlock, valid: Boolean, lastApplied: ModifierId): (EhrBlockStream, History.ProgressInfo[EhrBlock]) =
    this -> ProgressInfo(branchPoint = None,
      toRemove = Seq[EhrBlock](),
      toApply = None,
      toDownload = Seq[(ModifierTypeId, ModifierId)]())

  override def isSemanticallyValid(modifierId: ModifierId): ModifierSemanticValidity.Value =
    modifierById(modifierId).map { _ =>
      ModifierSemanticValidity.Valid
    }.getOrElse(ModifierSemanticValidity.Absent)

  override def openSurfaceIds(): Seq[ModifierId] =
    if (isEmpty) Seq[ModifierId]()
    else headOption.map(e => Seq(e.block.id)).getOrElse(Seq[ModifierId]())

  override def continuationIds(info: EhrSyncInfo, size: Int): Option[ModifierIds] =
    info.startingPoints.headOption.map { case (typeId, blockId) =>
      this.takeWhile(e => e.block.id != blockId).toList
        .reverse
        .take(size)
        .flatMap(e => Seq((typeId, e.block.id)))
    }

  override def syncInfo: EhrSyncInfo = new EhrSyncInfo(headOption.map(_.block.id))

  override def compare(other: EhrSyncInfo): History.HistoryComparisonResult.Value = {
    other.startingPoints.headOption.map{ case (_, blockId) =>
      find(_.block.id.mkString == blockId.mkString) match {
        case None => HistoryComparisonResult.Older
        case Some(element) if element.blockHeight == headBlockHeight => HistoryComparisonResult.Equal
        case _ => HistoryComparisonResult.Younger
      }
    }.getOrElse(History.HistoryComparisonResult.Nonsense)
  }

  /**
    * Not stack-safe. Eagerness controlled by binary operator `f`.
    *
    * @param z - the start value
    * @param f - the binary operator
    * @tparam B - the result type of the binary operator `f`
    */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def foldRight[B](z: => B)(f: (EhrBlockStreamElement, => B) => B): B = this match {
      case Cons(h,t) => f(h(), t().foldRight(z)(f))
      case _ => z
    }

  def headOption: Option[EhrBlockStreamElement] =
    foldRight[Option[EhrBlockStreamElement]](None)((a, _) => Some(a))

  @tailrec
  final def lastOption: Option[EhrBlockStreamElement] = this match {
    case Cons(h, t) if t() == empty => Some(h())
    case Cons(_, t) => t().lastOption
  }

  /**
    * Not stack-safe
    *
    * @param n - number of elements
    * @return - new stream with first `n` elements taken
    */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def take(n: Long): EhrBlockStream = this match {
    case Nil => Nil
    case Cons(_, _) if n == 0 => Nil
    case Cons(h, t) => cons(h(), t().take(n - 1))
  }

  def takeWhile(p: EhrBlockStreamElement => Boolean): EhrBlockStream =
    foldRight(empty)((a, b) => if (p(a)) cons(a, b) else empty)

  def toList: List[EhrBlockStreamElement] =
    foldRight(List[EhrBlockStreamElement]())((a, b) => a :: b)

  /**
    * Stream with first `n` elements skipped
    *
    * @param n - number of elements
    * @return - new stream with first `n` element skipped
    */
  @tailrec
  final def drop(n: Long): EhrBlockStream = this match {
    case Nil => Nil
    case Cons(h, t) if n == 0 => cons(h(), t())
    case Cons(_, t) => t().drop(n - 1)
  }

  @tailrec
  final def find(p: EhrBlockStreamElement => Boolean): Option[EhrBlockStreamElement] = this match {
    case Nil => None
    case Cons(h, t) => if (p(h())) Some(h()) else t().find(p)
  }
}

case object Nil extends EhrBlockStream

final case class Cons(h: () => EhrBlockStreamElement, t: () => EhrBlockStream)(implicit store: EhrHistoryStorage) extends EhrBlockStream {
  override def storage: EhrHistoryStorage = store
}

// todo use closure returning EhrBlock?
final case class EhrBlockStreamElement(block: EhrBlock, blockHeight: Long)

object EhrBlockStream {

  def cons(hd: => EhrBlockStreamElement, tl: => EhrBlockStream)(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty: EhrBlockStream = Nil

  @SuppressWarnings(Array("org.wartremover.warts.Recursion", "org.wartremover.warts.OptionPartial"))
  def load(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    def loop(blockId: () => ModifierId, height: Long): TailRec[EhrBlockStream] = {
      if (height > 0)
        tailcall(loop(() => storage.modifierById(blockId()).get.parentId, height - 1)
          .map( rest =>
            cons(EhrBlockStreamElement(storage.modifierById(blockId()).get, height), rest)
          )
        )
      else
        done(cons(EhrBlockStreamElement(storage.modifierById(blockId()).get, height), empty))
    }
    storage.bestBlockId.map( blockId =>
      loop(() => blockId, storage.height).result
    ).getOrElse(empty)
  }
}
