package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import scorex.core.{ModifierId, ModifierTypeId, NodeViewModifier}
import scorex.core.consensus.{History, ModifierSemanticValidity}
import scorex.core.consensus.History.{ModifierIds, ProgressInfo}
import scorex.core.utils.ScorexLogging
import scorex.crypto.encode.Base58

import scala.util.{Failure, Try}

trait EhrBlockStream extends History[EhrBlock, EhrSyncInfo, EhrBlockStream]
  with ScorexLogging {

  import EhrBlockStream._

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  override type NVCT = this.type

  def height: Long = headOption.map(_.height).getOrElse(0L)

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

  override def continuationIds(info: EhrSyncInfo, size: Int): Option[ModifierIds] = ???

  override def syncInfo: EhrSyncInfo = ???

  override def compare(other: EhrSyncInfo): History.HistoryComparisonResult.Value = ???

  def headOption: Option[EhrBlockStreamElement] = this match {
    case Cons(h, _) => Some(h())
    case Nil => None
  }
}

case object Nil extends EhrBlockStream {

  override def isEmpty: Boolean = true

  override def height: Long = 0

  override def modifierById(modifierId: ModifierId): Option[EhrBlock] = None

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  override def append(modifier: EhrBlock): Try[(EhrBlockStream, ProgressInfo[EhrBlock])] =
    Failure(new Exception("unexpected append call for Nil"))
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
final case class Cons(h: () => EhrBlockStreamElement, t: () => EhrBlockStream)(implicit storage: EhrHistoryStorage) extends EhrBlockStream {
  import EhrBlockStream._

  override def isEmpty: Boolean = false

  override def modifierById(modifierId: ModifierId): Option[EhrBlock] = storage.modifierById(modifierId)

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  override def append(block: EhrBlock): Try[(EhrBlockStream, History.ProgressInfo[EhrBlock])] = {
    require(height == storage.height, "append can be called on full stream")
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
}

final case class EhrBlockStreamElement(block: EhrBlock, height: Long)

object EhrBlockStream {
  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def cons(hd: => EhrBlockStreamElement, tl: => EhrBlockStream)(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty: EhrBlockStream = Nil
}
