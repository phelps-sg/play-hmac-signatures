package com.mesonomics.playhmacsignatures

import com.google.inject.Inject
import play.api.Configuration
import play.api.mvc.BodyParsers

import scala.concurrent.ExecutionContext

class SlackSignatureVerifyAction @Inject() (
    parser: BodyParsers.Default,
    config: Configuration,
    signatureVerifierService: SignatureVerifierService
)(implicit ec: ExecutionContext)
    extends SignatureVerifyAction(parser, config, signatureVerifierService) {
  override val headerKeyTimestamp: String = "X-Slack-Request-Timestamp"
  override val headerKeySignature: String = "X-Slack-Signature"
  override val signingSecretConfigKey: String = "slack.signingSecret"
}
