package ehr.record

import java.net.{ConnectException, InetSocketAddress}

import akka.actor.ActorSystem
import akka.testkit
import akka.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import ehr.EhrGenerators
import ehr.record.RecordFileDownloaderSupervisor.{DownloadFailed, DownloadSucceeded, DownloadErrors, NoPeers}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scorex.core.app.Version
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
    val peerManager = testkit.TestProbe()
    val supervisor = TestProbe[RecordFileDownloaderSupervisor.Command]()
    val fileStorage = new InMemoryRecordFileStorage()
    val actor = spawn(
      RecordFileDownloader.behavior(
        fileStorage,
        peerManager.ref) { (_, _, _) =>
        fail("download effect is called")
      }
    )
    val fileHash = InMemoryRecordFileStorageMock.recordFileHash
    actor ! RecordFileDownloader.DownloadFile(fileHash, supervisor.ref)
    peerManager.expectMsg(GetConnectedPeers)
    peerManager.reply(Seq[Handshake]())
    supervisor.expectMessage(DownloadFailed(fileHash, NoPeers))
  }

  it should "successful download" in {
    val peerManager = testkit.TestProbe()
    val supervisor = TestProbe[RecordFileDownloaderSupervisor.Command]()
    val fileStorage = new InMemoryRecordFileStorage()
    val actor = spawn(
      RecordFileDownloader.behavior(
        fileStorage,
        peerManager.ref) { (_, _, _) =>
        Right()
      }
    )
    val fileHash = InMemoryRecordFileStorageMock.recordFileHash
    actor ! RecordFileDownloader.DownloadFile(fileHash,
      supervisor.ref)
    peerManager.expectMsg(GetConnectedPeers)
    peerManager.reply(Seq[Handshake](
      Handshake("", Version(1, 2, 3), "",
        Some(new InetSocketAddress("92.92.92.92",27017)), 0L)))
    supervisor.expectMessage(DownloadSucceeded(fileHash))
  }

  it should "handle a failed download" in {
    val peerManager = testkit.TestProbe()
    val supervisor = TestProbe[RecordFileDownloaderSupervisor.Command]()
    val fileStorage = new InMemoryRecordFileStorage()
    val expectedException = new ConnectException()
    val actor = spawn(
      RecordFileDownloader.behavior(
        fileStorage,
        peerManager.ref) { (_, _, _) =>
        Left(expectedException)
      }
    )
    val fileHash = InMemoryRecordFileStorageMock.recordFileHash
    actor ! RecordFileDownloader.DownloadFile(fileHash,
      supervisor.ref)
    peerManager.expectMsg(GetConnectedPeers)
    peerManager.reply(Seq[Handshake](
      Handshake("", Version(1, 2, 3), "",
        Some(new InetSocketAddress("92.92.92.92",27017)), 0L)))
    supervisor.expectMessage(DownloadFailed(fileHash, DownloadErrors(Seq(expectedException))))
  }
}
