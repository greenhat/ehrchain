package ehr.api.http

import java.net.InetSocketAddress

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import ehr.record.InMemoryRecordFileStorageMock
import org.scalatest.{FlatSpec, Matchers}
import scorex.core.api.http.Stubs
import scorex.core.settings.RESTApiSettings

import scala.concurrent.duration._
import scala.language.postfixOps

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class FileApiRouteSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with Stubs {

  private implicit val timeout: RouteTestTimeout = RouteTestTimeout(15.seconds dilated)

  private val addr = new InetSocketAddress("localhost", 8080)
  private val restApiSettings = RESTApiSettings(addr, None, None, 10 seconds)
  private val prefix = "/file"
  private val fileHash = InMemoryRecordFileStorageMock.recordFileHash
  private val fileBytes = InMemoryRecordFileStorageMock.recordFileBytes
  private val fileStorage = InMemoryRecordFileStorageMock.storage
  private val routes = FileApiRoute(restApiSettings, fileStorage).route

  it should "serve file" in {
    Get(prefix + s"/hash/$fileHash") ~> routes ~> check {
      status shouldBe StatusCodes.OK
      responseAs[Array[Byte]] shouldEqual fileBytes
    }
  }

  it should "fail on invalid hash" in {
    Get(prefix + s"/hash/1111") ~> routes ~> check {
      status shouldBe StatusCodes.BadRequest
    }
  }
}
