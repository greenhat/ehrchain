package com.ehrchain.history

import com.ehrchain.block.EhrBlock
import com.ehrchain.mining.EhrMiningSettings
import scorex.core.{ModifierId, ModifierTypeId, NodeViewModifier}
import scorex.core.consensus.{History, ModifierSemanticValidity}
import scorex.core.consensus.History.{ModifierIds, ProgressInfo}
import scorex.core.utils.ScorexLogging
import scorex.crypto.encode.Base58

import scala.util.{Failure, Try}

class EhrHistory(val storage: EhrHistoryStorage,
                 settings: EhrMiningSettings)
  extends History[EhrBlock, EhrSyncInfo, EhrHistory] with ScorexLogging {

  override type NVCT = this.type

  require(NodeViewModifier.ModifierIdSize == 32, "32 bytes ids assumed")

  //noinspection CorrespondsUnsorted
//  private def isGenesis(block: EhrBlock): Boolean = block.parentId sameElements GenesisParentId

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  override def append(block: EhrBlock): Try[(EhrHistory, History.ProgressInfo[EhrBlock])] = {
    log.debug(s"Trying to append block ${Base58.encode(block.id)} to history")
    if (block.validity) {
      storage.update(block)
      Try {
        (new EhrHistory(storage, settings),
          ProgressInfo(branchPoint = None,
            toRemove = Seq[EhrBlock](),
            toApply = Some(block),
            toDownload = Seq[(ModifierTypeId, ModifierId)]())
        )
      }
    } else Failure(new Exception("block is not valid"))
  }

  override def reportSemanticValidity(modifier: EhrBlock,
                                      valid: Boolean,
                                      lastApplied: ModifierId): (EhrHistory, History.ProgressInfo[EhrBlock]) = this -> ProgressInfo(branchPoint = None,
      toRemove = Seq[EhrBlock](),
      toApply = None,
      toDownload = Seq[(ModifierTypeId, ModifierId)]())

  /**
    * Is there's no history, even genesis block
    */
  override def isEmpty: Boolean = storage.height <= 0

  override def modifierById(modifierId: ModifierId): Option[EhrBlock] = storage.modifierById(modifierId)

  override def isSemanticallyValid(modifierId: ModifierId): ModifierSemanticValidity.Value =
    modifierById(modifierId).map { _ =>
      ModifierSemanticValidity.Valid
    }.getOrElse(ModifierSemanticValidity.Absent)

  override def openSurfaceIds(): Seq[ModifierId] =
    if (isEmpty) Seq(settings.GenesisParentId)
    else Seq(storage.bestBlockId)

  /**
    * Ids of modifiers, that node with info should download and apply to synchronize
    */
  override def continuationIds(info: EhrSyncInfo, size: Int): Option[ModifierIds] = ???

  /**
    * Information about our node synchronization status. Other node should be able to compare it's view with ours by
    * this syncInfo message and calculate modifiers missed by our node.
    *
    * @return
    */
  override def syncInfo: EhrSyncInfo = ???

  /**
    * Whether another's node syncinfo shows that another node is ahead or behind ours
    *
    * @param other other's node sync info
    * @return Equal if nodes have the same history, Younger if another node is behind, Older if a new node is ahead
    */
  override def compare(other: EhrSyncInfo): History.HistoryComparisonResult.Value = ???

}
