package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import scorex.core.{ModifierId, ModifierTypeId, NodeViewModifier}
import scorex.core.consensus.{History, ModifierSemanticValidity}
import scorex.core.consensus.History.{ModifierIds, ProgressInfo}
import scorex.core.utils.ScorexLogging
import scorex.crypto.encode.Base58

import scala.annotation.tailrec
import scala.util.{Failure, Try}

trait EhrBlockStream extends History[EhrBlock, EhrSyncInfo, EhrBlockStream]
  with ScorexLogging {

  import EhrBlockStream._

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  override type NVCT = this.type

  implicit def storage: EhrHistoryStorage = ???

  def height: Long = headOption.map(_.height).getOrElse(0L)

  import EhrBlockStream._

  override def isEmpty: Boolean = height == 0

  override def modifierById(modifierId: ModifierId): Option[EhrBlock] = storage.modifierById(modifierId)

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  override def append(block: EhrBlock): Try[(EhrBlockStream, History.ProgressInfo[EhrBlock])] = {
    require(height == storage.height, "append must be called on full stream")
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
    } else Failure(new Exception("block is not valid"))
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

  override def syncInfo: EhrSyncInfo = ???

  override def compare(other: EhrSyncInfo): History.HistoryComparisonResult.Value = ???

  def headOption: Option[EhrBlockStreamElement] = this match {
    case Cons(h, _) => Some(h())
    case Nil => None
  }

  @tailrec
  final def lastOption: Option[EhrBlockStreamElement] = this match {
    case Cons(h, t) if t() == empty => Some(h())
    case Cons(_, t) => t().lastOption
  }

  // fixme trampolines?
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def take(n: Long): EhrBlockStream = this match {
    case Nil => Nil
    case Cons(_, _) if n == 0 => Nil
    case Cons(h, t) => cons(h(), t().take(n - 1))
  }

  // fixme trampolines?
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def takeWhile(p: EhrBlockStreamElement => Boolean): EhrBlockStream = this match {
    case Nil => Nil
    case Cons(h, t) if p(h()) => cons(h(), t().takeWhile(p))
    case _ => Nil
  }

  // fixme trampolines?
  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def toList: List[EhrBlockStreamElement] = this match {
    case Nil => List()
    case Cons(h, t) => h() :: t().toList
  }
}

case object Nil extends EhrBlockStream

final case class Cons(h: () => EhrBlockStreamElement, t: () => EhrBlockStream)(implicit store: EhrHistoryStorage) extends EhrBlockStream {
  override def storage: EhrHistoryStorage = store
}

final case class EhrBlockStreamElement(block: EhrBlock, height: Long)

object EhrBlockStream {

  def cons(hd: => EhrBlockStreamElement, tl: => EhrBlockStream)(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty: EhrBlockStream = Nil

  @SuppressWarnings(Array("org.wartremover.warts.Recursion", "org.wartremover.warts.OptionPartial"))
  def load(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    def loop(blockId: () => ModifierId, height: Long): EhrBlockStream = {
      if (height > 0)
        cons(EhrBlockStreamElement(storage.modifierById(blockId()).get, height),
          loop(() => storage.modifierById(blockId()).get.parentId, height - 1))
      else
        cons(EhrBlockStreamElement(storage.modifierById(blockId()).get, height), empty)
    }
    storage.bestBlockId.map( blockId =>
      loop(() => blockId, storage.height)
    ).getOrElse(empty)
  }
}
