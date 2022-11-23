/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import com.mesonomics.playhmacsignatures.{
  InvalidSignatureException,
  SignatureVerifierService,
  SlackSignatureVerifyAction
}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{POST, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ControllerTests
    extends AnyWordSpecLike
    with should.Matchers
    with MockFactory {

  "TestController" should {

    val mockService = mock[SignatureVerifierService]
    val config = Configuration("slack.signingSecret" -> "test-secret")
    implicit val system: ActorSystem = ActorSystem("ControllerTests")
    implicit val mat: Materializer = Materializer(system)
    val bp = new BodyParsers.Default()

    val slackSignatureVerifyAction = new SlackSignatureVerifyAction(
      bp,
      config,
      mockService
    )

    val testController = new TestController(
      Helpers.stubControllerComponents(),
      slackSignatureVerifyAction
    )

    val message = Json.parse(""" { "message" : "Hello world!" } """).toString()
    val body = ByteString(message)

    val signatureHeaders = Array(
      ("X-Slack-Request-Timestamp", "1663156082"),
      (
        "X-Slack-Signature",
        "v0=d1c387a20da72e5e07de4e2fb7e93cd9b44c2caa118868aad99c3b20c93de73a"
      )
    )

    "return a 401 error when not supplying signatures" in {
      val fakeRequest = FakeRequest(POST, "/").withBody(body)
      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

    "return a 401 error when supplying empty timestamp" in {
      val fakeRequest = FakeRequest(POST, "/").withBody(body)
      val headersWithEmptyTimestamp = Array(
        ("X-Slack-Request-Timestamp", ""),
        (
          "X-Slack-Signature",
          "v0=d1c387a20da72e5e07de4e2fb7e93cd9b44c2caa118868aad99c3b20c93de73a"
        )
      )
      val result = testController
        .test()
        .apply(fakeRequest.withHeaders(headersWithEmptyTimestamp: _*))
      status(result) mustEqual UNAUTHORIZED
    }

    "return a 401 error when supplying non-numeric timestamp" in {
      val fakeRequest = FakeRequest(POST, "/").withBody(body)
      val headersWithEmptyTimestamp = Array(
        ("X-Slack-Request-Timestamp", "non-numeric"),
        (
          "X-Slack-Signature",
          "v0=d1c387a20da72e5e07de4e2fb7e93cd9b44c2caa118868aad99c3b20c93de73a"
        )
      )
      val result = testController
        .test()
        .apply(fakeRequest.withHeaders(headersWithEmptyTimestamp: _*))
      status(result) mustEqual UNAUTHORIZED
    }

    "return a 401 error when supplying invalid signatures" in {

      (mockService
        .validate(_: Clock)(_: Duration)(_: (Long, ByteString) => String)(
          _: Array[Byte] => ByteString
        )(_: String)(
          _: Long,
          _: ByteString,
          _: ByteString
        ))
        .expects(*, *, *, *, *, *, *, *)
        .returning(Failure(InvalidSignatureException))

      val fakeRequest = FakeRequest(POST, "/")
        .withBody(body)
        .withHeaders(signatureHeaders: _*)

      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

    "return success when supplying valid signatures" in {

      (mockService
        .validate(_: Clock)(_: Duration)(_: (Long, ByteString) => String)(
          _: Array[Byte] => ByteString
        )(_: String)(
          _: Long,
          _: ByteString,
          _: ByteString
        ))
        .expects(*, *, *, *, *, *, *, *)
        .returning(Success(ByteString(message)))

      val fakeRequest = FakeRequest(POST, "/")
        .withBody(body)
        .withHeaders(signatureHeaders: _*)

      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual OK
    }

  }
}
