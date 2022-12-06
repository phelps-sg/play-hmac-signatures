/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mesonomics.playhmacsignatures.{
  SignatureVerifierService,
  SlackSignatureVerifyAction
}
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.mvc.BodyParsers

import scala.concurrent.ExecutionContext.Implicits.global

trait CommonFixtures extends MockFactory {
  val mockService: SignatureVerifierService = mock[SignatureVerifierService]
  val config: Configuration = Configuration(
    "slack.signingSecret" -> "test-secret"
  )
  implicit val system: ActorSystem = ActorSystem("ControllerTests")
  implicit val mat: Materializer = Materializer(system)
  val bp = new BodyParsers.Default()

  val slackSignatureVerifyAction = new SlackSignatureVerifyAction(
    bp,
    config,
    mockService
  )
}
