/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.util.ByteString
import com.mesonomics.playhmacsignatures.{
  EpochSeconds,
  HmacSignature,
  InvalidSignatureException
}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{POST, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class ControllerTests
    extends AnyWordSpecLike
    with should.Matchers
    with MockFactory {

  trait TestFixtures extends CommonFixtures {

    val testController: TestController = new TestController(
      Helpers.stubControllerComponents(),
      slackSignatureVerifyAction
    )

    val messageJson: JsValue =
      Json.parse(""" { "message" : "Hello world!" } """)
    val message: String = messageJson.toString()
    val body: ByteString = ByteString(message)

    val signatureHeaders: Array[(String, String)] = Array(
      ("X-Slack-Request-Timestamp", "1663156082"),
      (
        "X-Slack-Signature",
        "v0=d1c387a20da72e5e07de4e2fb7e93cd9b44c2caa118868aad99c3b20c93de73a"
      )
    )

  }

  "TestController" should {

    "return a 401 error when not supplying signatures" in new TestFixtures {
      val fakeRequest = FakeRequest(POST, "/").withBody(body)
      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

    "return a 401 error when supplying empty timestamp" in new TestFixtures {
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

    "return a 401 error when supplying non-numeric timestamp" in new TestFixtures {
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

    "return a 401 error when supplying invalid signatures" in new TestFixtures {

      (mockService
        .validate(_: Clock)(_: Duration)(
          _: (EpochSeconds, ByteString) => String
        )(
          _: Array[Byte] => HmacSignature
        )(_: String)(
          _: EpochSeconds,
          _: ByteString,
          _: HmacSignature
        ))
        .expects(*, *, *, *, *, *, *, *)
        .returning(Failure(InvalidSignatureException))

      val fakeRequest = FakeRequest(POST, "/")
        .withBody(body)
        .withHeaders(signatureHeaders: _*)

      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

    "return success when supplying valid signatures" in new TestFixtures {

      (mockService
        .validate(_: Clock)(_: Duration)(
          _: (EpochSeconds, ByteString) => String
        )(
          _: Array[Byte] => HmacSignature
        )(_: String)(
          _: EpochSeconds,
          _: ByteString,
          _: HmacSignature
        ))
        .expects(*, *, *, *, *, *, *, *)
        .returning(Success(ByteString(message)))

      val fakeRequest = FakeRequest(POST, "/")
        .withBody(body)
        .withHeaders(signatureHeaders: _*)

      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual OK
      val resultContents = contentAsJson(result)
      resultContents mustEqual messageJson("message")
    }

  }
}
