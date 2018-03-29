package ehr.record

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import akka.testkit.typed.scaladsl.{BehaviorTestKit, Effects}
import ehr.record.RecordFileDownloaderSupervisor.DownloadMissingFiles
import org.scalatest.{FlatSpec, Matchers}

@SuppressWarnings(Array("org.wartremover.warts.Nothing", "org.wartremover.warts.NonUnitStatements"))
class RecordFileDownloaderSupervisorSpec extends FlatSpec with Matchers {

implicit lazy val system: ActorSystem = ActorSystem()

  it should "don't spawn a downloader and terminate on empty txs" in {
    val peerManager = TestProbe()
    val fileStorage = new InMemoryRecordFileStorage()
    val testKit = BehaviorTestKit(
      RecordFileDownloaderSupervisor.behavior(
        fileStorage,
        peerManager.ref)
    )
    testKit.run(DownloadMissingFiles(Seq()))
    testKit.retrieveEffect() shouldEqual Effects.NoEffects
    testKit.isAlive shouldBe false
  }
}
