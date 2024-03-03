/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

import akka.util.ByteString
import com.google.inject.Inject
import com.mesonomics.playhmacsignatures.SlackSignatureVerifyAction.convertBytesToHex
import play.api.Configuration
import play.api.mvc.BodyParsers

import scala.concurrent.ExecutionContext

object SlackSignatureVerifyAction {
    def convertBytesToHex(bytes: Seq[Byte]): String = {
      val sb = new StringBuilder
      for (b <- bytes) {
        sb.append(String.format("%02x", Byte.box(b)))
      }
      sb.toString
    }
}
/** This class can be used to validate signatures in
  * [[https://api.slack.com/authentication/verifying-requests-from-slack#verifying-requests-from-slack-using-signing-secrets__a-recipe-for-security__step-by-step-walk-through-for-validating-a-request Slack requests]].
  *
  * @param parser
  *   The body parser
  * @param config
  *   The play configuration instance
  * @param signatureVerifierService
  *   The service used to validate the signature against the body
  * @param ec
  *   The execution context
  */
class SlackSignatureVerifyAction @Inject() (
    parser: BodyParsers.Default,
    config: Configuration,
    signatureVerifierService: SignatureVerifierService
)(implicit ec: ExecutionContext)
    extends SignatureVerifyAction(parser, config, signatureVerifierService) {

  override val headerKeyTimestamp: String = "X-Slack-Request-Timestamp"
  override val headerKeySignature: String = "X-Slack-Signature"
  override val signingSecretConfigKey: String = "slack.signingSecret"

  override def payload(timestamp: EpochSeconds, body: ByteString): String = {
    s"v0:${timestamp.value}:${body.utf8String}"
  }

  override def expectedSignature(macBytes: Array[Byte]): HmacSignature = {
    HmacSignature(
      s"v0=${convertBytesToHex(macBytes)}"
    )
  }
}
