/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

import akka.util.ByteString
import com.google.inject.Inject
import play.api.Configuration
import play.api.mvc.BodyParsers

import javax.xml.bind.DatatypeConverter
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

  override def payload(timestamp: Long, body: ByteString): String =
    s"v0:$timestamp:${body.utf8String}"

  override def expectedSignature(macBytes: Array[Byte]): ByteString =
    ByteString(
      s"v0=${DatatypeConverter.printHexBinary(macBytes).toLowerCase}"
    )
}
