import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import net.sphelps.playhmacsignatures.{
  HmacSHA256SignatureVerifier,
  SlackSignatureVerifyAction
}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.Helpers.{POST, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}

import scala.collection.immutable.ArraySeq
import scala.concurrent.ExecutionContext.Implicits.global

class ControllerTests extends AnyWordSpecLike with should.Matchers {

  "TestController" should {

    val service = new HmacSHA256SignatureVerifier()
    val config = Configuration("slack.signingSecret" -> "test-secret")
    implicit val system: ActorSystem = ActorSystem("ControllerTests")
    implicit val mat: Materializer = Materializer(system)
    val bp = new BodyParsers.Default()
    val slackSignatureVerifyAction = new SlackSignatureVerifyAction(
      bp,
      config,
      service
    )
    val testController = new TestController(
      Helpers.stubControllerComponents(),
      slackSignatureVerifyAction
    )
    val body = ByteString(
        Json.parse(""" { "message" : "Hello world!" } """).toString()
      )

    "return a 401 error when not supplying signatures" in {
     val fakeRequest = FakeRequest(POST, "/").withBody(body)
      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

    "return a 401 error when supplying invalid signatures" in {
      val invalidSignatureHeaders = Array(
        ("X-Slack-Request-Timestamp", "1663156082"),
        (
          "X-Slack-Signature",
          "v0=d1c387a20da72e5e07de4e2fb7e93cd9b44c2caa118868aad99c3b20c93de73a"
        )
      )
      val fakeRequest = FakeRequest(POST, "/")
        .withBody(body)
        .withHeaders(
          ArraySeq.unsafeWrapArray(invalidSignatureHeaders): _*
        )
      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

  }
}
