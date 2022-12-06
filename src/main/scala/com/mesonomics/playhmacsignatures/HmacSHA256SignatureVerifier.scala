/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

import akka.util.ByteString
import com.google.inject.{ImplementedBy, Singleton}
import play.api.Logging

import java.time.Clock
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.duration.Duration
import scala.math.abs
import scala.util.{Failure, Success, Try}

case object InvalidSignatureException extends Exception("Invalid signature")
case object InvalidTimestampException extends Exception("Invalid timestamp")

final case class EpochSeconds(value: Long) extends AnyVal

final case class HmacSignature(value: ByteString) extends AnyVal
object HmacSignature {
  def apply(value: String): HmacSignature = HmacSignature(ByteString(value))
}

@ImplementedBy(classOf[HmacSHA256SignatureVerifier])
trait SignatureVerifierService {
  def validate(
      clock: Clock
  )(
      timestampTolerance: Duration
  )(
      payload: (EpochSeconds, ByteString) => String
  )(
      expectedSignature: Array[Byte] => HmacSignature
  )(
      signingSecret: String
  )(
      timestamp: EpochSeconds,
      body: ByteString,
      signature: HmacSignature
  ): Try[ByteString]
}

@Singleton
class HmacSHA256SignatureVerifier
    extends SignatureVerifierService
    with Logging {

  val algorithm = "HmacSHA256"

  def validate(
      clock: Clock
  )(
      timestampTolerance: Duration
  )(
      payload: (EpochSeconds, ByteString) => String
  )(
      expectedSignature: Array[Byte] => HmacSignature
  )(
      signingSecret: String
  )(
      timestamp: EpochSeconds,
      body: ByteString,
      signature: HmacSignature
  ): Try[ByteString] =
    for {
      _ <- validateTimestamp(clock)(timestampTolerance)(timestamp)
      _ <- validateSignature(payload)(expectedSignature)(signingSecret)(
        timestamp,
        body,
        signature
      )
    } yield body

  protected def validateSignature(
      payload: (EpochSeconds, ByteString) => String
  )(
      expectedSignature: Array[Byte] => HmacSignature
  )(
      signingSecret: String
  )(
      timestamp: EpochSeconds,
      body: ByteString,
      signature: HmacSignature
  ): Try[ByteString] = {
    val secret = new SecretKeySpec(signingSecret.getBytes, algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(secret)
    val macBytes = mac.doFinal(payload(timestamp, body).getBytes)
    if (signature == expectedSignature(macBytes)) {
      Success(body)
    } else {
      logger.debug(s"Invalid signature ($timestamp, $body, $signature)")
      Failure(InvalidSignatureException)
    }
  }

  protected def validateTimestamp(
      clock: Clock
  )(
      tolerance: Duration
  )(
      timestamp: EpochSeconds
  ): Try[EpochSeconds] = {
    if (
      abs(
        timestamp.value - clock.instant().getEpochSecond
      ) <= tolerance.toSeconds
    ) {
      Success(timestamp)
    } else {
      logger.debug(s"Invalid timestamp ($timestamp, $tolerance, $clock)")
      Failure(InvalidTimestampException)
    }
  }
}
