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

/**
  * Lazy evaluated list of the blocks presenting a blockchain.
  * Blocks are not evaluated(loaded from the storage) until absolutely need to.
  * Once evaluated(loaded) blocks remain cached for subsequent access.
  */
trait EhrBlockStream extends History[EhrBlock, EhrSyncInfo, EhrBlockStream]
  with ScorexLogging {

  import EhrBlockStream._

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  override type NVCT = this.type

  /**
    * @note - must be overridden in case class
    * @return - underlying persistent storage for the block stream elements
    */
  implicit def storage: EhrHistoryStorage = ???

  def headBlockHeight: Long = headOption.map(_.blockHeight).getOrElse(0L)

  import EhrBlockStream._

  /**
    * Is there's no history, even genesis block
    */
  override def isEmpty: Boolean = headBlockHeight == 0

  /**
    * Return modifier(block) of type PM with id == modifierId
    *
    * @param modifierId - modifier id to get from history
    * @return
    */
  override def modifierById(modifierId: ModifierId): Option[EhrBlock] = storage.modifierById(modifierId)

  /**
    * Appends (prepends) a modifier/block to the stream(history)
    *
    * @return stream with appended block, and the description of the action;
    */
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

  /**
    * Report that modifier is valid from other nodeViewHolder components point of view
    */
  override def reportSemanticValidity(modifier: EhrBlock, valid: Boolean, lastApplied: ModifierId): (EhrBlockStream, History.ProgressInfo[EhrBlock]) =
    this -> ProgressInfo(branchPoint = None,
      toRemove = Seq[EhrBlock](),
      toApply = None,
      toDownload = Seq[(ModifierTypeId, ModifierId)]())

  /**
    * Return semantic validity status of modifier with id == modifierId
    *
    * @param modifierId - modifier id to check
    * @return - Valid if found, Absent otherwise
    */
  override def isSemanticallyValid(modifierId: ModifierId): ModifierSemanticValidity.Value =
    modifierById(modifierId).map { _ =>
      ModifierSemanticValidity.Valid
    }.getOrElse(ModifierSemanticValidity.Absent)

  /**
    * @return - last/best block id
    */
  override def openSurfaceIds(): Seq[ModifierId] =
    if (isEmpty) Seq[ModifierId]()
    else headOption.map(e => Seq(e.block.id)).getOrElse(Seq[ModifierId]())

  /**
    * Ids of modifiers, that node with given info should download and apply to synchronize
    */
  override def continuationIds(info: EhrSyncInfo, size: Int): Option[ModifierIds] =
    info.startingPoints.headOption.map { case (typeId, blockId) =>
      takeWhile(e => e.block.id != blockId).toList
        .reverse
        .take(size)
        .flatMap(e => Seq((typeId, e.block.id)))
    }

  /**
    * Information about our node synchronization status. Other node should be able to compare it's view with ours by
    * this syncInfo message and calculate modifiers missed by our node.
    */
  override def syncInfo: EhrSyncInfo = new EhrSyncInfo(headOption.map(_.block.id))

  /**
    * Given `other` node sync info compares it with ours and returns whether other node's position
    * on the blockchain comparing to ours (behind, ahead or equal)
    *
    * @param other other's node sync info
    * @return Equal if nodes have the same history, Younger if another node is behind, Older if a new node is ahead
    */
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
    * @return - head of the stream (last/best block on the blockchain)
    */
  def headOption: Option[EhrBlockStreamElement] = this match {
    case Nil => None
    case Cons(h, _) => Some(h())
  }

  /**
    * @return - last element in the stream
    */
  @tailrec
  final def lastOption: Option[EhrBlockStreamElement] = this match {
    case Cons(h, t) if t() == empty => Some(h())
    case Cons(_, t) => t().lastOption
  }

  /**
    * New stream with first `n` elements from the original stream
    *
    * @param n - number of elements
    * @return - new stream with first `n` elements taken
    */
  def take(n: Long): EhrBlockStream = {
    @tailrec
    def loop(rest: EhrBlockStream, taken: EhrBlockStream, takenElements: Long): EhrBlockStream = rest match {
      case Nil => taken
      case Cons(h, t) => if (takenElements < n) loop(t(), cons(h(), taken), takenElements + 1) else taken
    }
    loop(this, empty, 0)
  }

  /**
    * New stream with elements taken from this stream while predicate holds true
    *
    * @param p - predicate
    * @return - new stream
    */
  def takeWhile(p: EhrBlockStreamElement => Boolean): EhrBlockStream = {
    @tailrec
    def loop(rest: EhrBlockStream, taken: EhrBlockStream): EhrBlockStream = rest match {
      case Nil => taken
      case Cons(h, t) => if (p(h())) loop(t(), cons(h(), taken)) else taken
    }
    loop(this, empty)
  }

  /**
    * @return - List with all the elements in the stream
    *
    * @note - evaluates (loads) blocks from the underlying storage
    */
  def toList: List[EhrBlockStreamElement] = {
    @tailrec
    def loop(rest: EhrBlockStream, list: List[EhrBlockStreamElement]): List[EhrBlockStreamElement] = rest match {
      case Nil => list
      case Cons(h, t) => loop(t(), h() +: list)
    }
    loop(this, List()).reverse
  }

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

  /**
    * Finds the first element passing the given `p` predicate
    * @param p - predicate to apply to every element
    * @return - first element passing the `p` predicate
    */
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

  val GenesisParentId: ModifierId = ModifierId @@ Array.fill(32)(1: Byte)

  /**
    * "Smart" constructor, caches the elements being evaluated
    * @param hd - head element, lazy (passed by name)
    * @param tl - the rest of the stream, lazy (passed by name);
    * @param storage - underlying persistent storage for stream elements
    * @return - stream
    */
  def cons(hd: => EhrBlockStreamElement, tl: => EhrBlockStream)(implicit storage: EhrHistoryStorage): EhrBlockStream = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty: EhrBlockStream = Nil

  /**
    * Loads and constructs the stream from the underlying persistent storage. Stack safe.
    *
    * @param storage - persistent storage
    * @return - stream
    */
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
