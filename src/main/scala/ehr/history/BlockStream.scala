package ehr.history

import ehr.block.EhrBlock
import scorex.core.consensus.History._
import scorex.core.consensus.{Absent, History, ModifierSemanticValidity, Valid}
import scorex.core.utils.ScorexLogging
import scorex.core.{ModifierId, ModifierTypeId, NodeViewModifier}
import scorex.crypto.encode.Base58

import scala.annotation.tailrec
import scala.util.Try
import scala.util.control.TailCalls.{TailRec, done, tailcall}

/**
  * Lazy evaluated list of the blocks presenting a blockchain.
  * Blocks are not evaluated(loaded from the storage) until absolutely need to.
  * Once evaluated(loaded) blocks remain cached for subsequent access.
  */
trait BlockStream extends History[EhrBlock, EhrSyncInfo, BlockStream]
  with ScorexLogging {

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  override type NVCT = this.type

  /**
    * @note - must be overridden in case class
    * @return - underlying persistent storage for the block stream elements
    */
  implicit def storage: HistoryStorage = ???

  def headBlockHeight: Long = headOption.map(_.blockHeight).getOrElse(0L)

  import BlockStream._

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
  override def append(block: EhrBlock): Try[(BlockStream, History.ProgressInfo[EhrBlock])] = {
    log.debug(s"Trying to append block ${Base58.encode(block.id)} to history")
    for {
      _ <- block.validity
      _ <- Try { require(isGenesisBlock(block) || headOption.exists(_.block.id sameElements block.parentId),
        s"previous block id is missing (best block id: ${storage.bestBlockId.map(Base58.encode(_))}") }
    } yield {
      storage.append(block)
      (cons(EhrBlockStreamElement(block, storage.heightOf(block.id).getOrElse(0L)), this),
        ProgressInfo(branchPoint = None,
          toRemove = Seq[EhrBlock](),
          toApply = Seq(block),
          toDownload = Seq[(ModifierTypeId, ModifierId)]())
      )
    }
  }

  /**
    * Report that modifier is valid from point of view of the state component
    *
    * @param modifier - valid modifier
    * @return modified history
    */
  override def reportModifierIsValid(modifier: EhrBlock): BlockStream = this

  /**
    * Report that modifier is invalid from other nodeViewHolder components point of view
    *
    * @param modifier     - invalid modifier
    * @param progressInfo - what suffix failed to be applied because of an invalid modifier
    * @return modified history and new progress info
    */
  override def reportModifierIsInvalid(modifier: EhrBlock,
                                       progressInfo: ProgressInfo[EhrBlock]
                                      ): (BlockStream, ProgressInfo[EhrBlock]) =
  // todo remove the sub-chain(prefix) starting with invalid modifier
    this -> ProgressInfo(branchPoint = None,
      toRemove = Seq[EhrBlock](),
      toApply = Seq[EhrBlock](),
      toDownload = Seq[(ModifierTypeId, ModifierId)]())

  /**
    * Return semantic validity status of modifier with id == modifierId
    *
    * @param modifierId - modifier id to check
    * @return - Valid if found, Absent otherwise
    */
  override def isSemanticallyValid(modifierId: ModifierId): ModifierSemanticValidity =
    modifierById(modifierId).map { _ =>
      Valid
    }.getOrElse(Absent)

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
  override def compare(other: EhrSyncInfo): History.HistoryComparisonResult = {
    other.startingPoints.headOption.map{ case (_, blockId) =>
      find(_.block.id.mkString == blockId.mkString) match {
        case None => Older
        case Some(element) if element.blockHeight == headBlockHeight => Equal
        case _ => Younger
      }
    }.getOrElse(Nonsense)
  }

  /**
    * @return - head of the stream (last/best block on the blockchain)
    */
  def headOption: Option[EhrBlockStreamElement] = this match {
    case Nil() => None
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
  def take(n: Long): BlockStream = {
    @tailrec
    def loop(rest: BlockStream, taken: BlockStream, takenElements: Long): BlockStream = rest match {
      case Nil() => taken
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
  def takeWhile(p: EhrBlockStreamElement => Boolean): BlockStream = {
    @tailrec
    def loop(rest: BlockStream, taken: BlockStream): BlockStream = rest match {
      case Nil() => taken
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
    def loop(rest: BlockStream, list: List[EhrBlockStreamElement]): List[EhrBlockStreamElement] = rest match {
      case Nil() => list
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
  final def drop(n: Long): BlockStream = this match {
    case Nil() => Nil()
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
    case Nil() => None
    case Cons(h, t) => if (p(h())) Some(h()) else t().find(p)
  }
}

final case class Nil()(implicit store: HistoryStorage) extends BlockStream {
  override def storage: HistoryStorage = store
}

final case class Cons(h: () => EhrBlockStreamElement, t: () => BlockStream)(implicit store: HistoryStorage) extends BlockStream {
  override def storage: HistoryStorage = store
}

final case class EhrBlockStreamElement(block: EhrBlock, blockHeight: Long)

object BlockStream {

  val GenesisParentId: ModifierId = ModifierId @@ Array.fill(32)(1: Byte)

  /**
    * "Smart" constructor, caches the elements being evaluated
    * @param hd - head element, lazy (passed by name)
    * @param tl - the rest of the stream, lazy (passed by name);
    * @param storage - underlying persistent storage for stream elements
    * @return - stream
    */
  def cons(hd: => EhrBlockStreamElement, tl: => BlockStream)(implicit storage: HistoryStorage): BlockStream = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty(implicit storage: HistoryStorage): BlockStream = Nil()

  /**
    * Loads and constructs the stream from the underlying persistent storage. Stack safe.
    *
    * @param storage - persistent storage
    * @return - stream
    */
  @SuppressWarnings(Array("org.wartremover.warts.Recursion", "org.wartremover.warts.OptionPartial"))
  def load(implicit storage: HistoryStorage): BlockStream = {
    def loop(blockId: () => ModifierId, height: Long): TailRec[BlockStream] = {
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
    ).getOrElse(empty(storage))
  }

  def isGenesisBlock(block: EhrBlock): Boolean = block.parentId sameElements GenesisParentId
}
