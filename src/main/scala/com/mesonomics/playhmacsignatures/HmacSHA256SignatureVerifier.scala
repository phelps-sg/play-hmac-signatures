/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

import akka.util.ByteString
import com.google.inject.{ImplementedBy, Singleton}

import java.time.Clock
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.duration.Duration
import scala.math.abs
import scala.util.{Failure, Success, Try}

case object InvalidSignatureException extends Exception("Invalid signature")
case object InvalidTimestampException extends Exception("Invalid timestamp")

final case class EpochSeconds(value: Long) extends AnyVal

@ImplementedBy(classOf[HmacSHA256SignatureVerifier])
trait SignatureVerifierService {
  def validate(clock: Clock)(timestampTolerance: Duration)(
      payload: (EpochSeconds, ByteString) => String
  )(expectedSignature: Array[Byte] => ByteString)(signingSecret: String)(
      timestamp: EpochSeconds,
      body: ByteString,
      signature: ByteString
  ): Try[ByteString]
}

@Singleton
class HmacSHA256SignatureVerifier extends SignatureVerifierService {

  val algorithm = "HmacSHA256"

  def validate(
      clock: Clock
  )(
      timestampTolerance: Duration
  )(
      payload: (EpochSeconds, ByteString) => String
  )(
      expectedSignature: Array[Byte] => ByteString
  )(
      signingSecret: String
  )(
      timestamp: EpochSeconds,
      body: ByteString,
      signature: ByteString
  ): Try[ByteString] = {

    if (
      abs(
        timestamp.value - clock.instant().getEpochSecond
      ) > timestampTolerance.toSeconds
    ) {
      Failure(InvalidTimestampException)
    } else {
      val secret = new SecretKeySpec(signingSecret.getBytes, algorithm)
      val mac = Mac.getInstance(algorithm)
      mac.init(secret)
      val macBytes = mac.doFinal(payload(timestamp, body).getBytes)
      if (signature == expectedSignature(macBytes)) {
        Success(body)
      } else {
        Failure(InvalidSignatureException)
      }
    }
  }
}
