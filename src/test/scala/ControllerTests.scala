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

    "return a 401 error when not supplying signatures" in {
      val body = ByteString(
        Json.parse(""" { "message" : "Hello world!" } """).toString()
      )
      val fakeRequest = FakeRequest(POST, "/").withBody(body)
      val result = testController.test().apply(fakeRequest)
      status(result) mustEqual UNAUTHORIZED
    }

  }
}
