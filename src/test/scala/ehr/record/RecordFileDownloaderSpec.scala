package ehr.record

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import akka.testkit.typed.scaladsl.ActorTestKit
import ehr.EhrGenerators
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scorex.core.network.Handshake
import scorex.core.network.peer.PeerManager.ReceivableMessages.GetConnectedPeers

@SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.NonUnitStatements",
  "org.wartremover.warts.OptionPartial"))
class RecordFileDownloaderSpec extends FlatSpec
  with ActorTestKit
  with BeforeAndAfterAll
  with Matchers
  with EhrGenerators {

  override def afterAll(): Unit = shutdownTestKit()

  implicit lazy val untypedSystem: ActorSystem = ActorSystem()

  it should "terminate if no peers" in {
    val peerManager = TestProbe()
    val fileStorage = new InMemoryRecordFileStorage()
    val actor = spawn(
      RecordFileDownloader.behavior(
        fileStorage,
        peerManager.ref) { (_, _, _) =>
        fail("download effect is called")
      }
    )
    actor ! RecordFileDownloader.DownloadFile(InMemoryRecordFileStorageMock.recordFileHash)
    peerManager.expectMsg(GetConnectedPeers)
    peerManager.reply(Seq[Handshake]())
  }
}
