/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.util.ByteString
import com.mesonomics.playhmacsignatures._
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{Clock, OffsetDateTime}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Success}

class ServiceTests extends AnyWordSpecLike with should.Matchers {

  val timestampStr: String = "2017-09-15T13:50:30.526+05:30"
  val offsetTime: OffsetDateTime = OffsetDateTime.parse(timestampStr)
  val timestamp: EpochSeconds = EpochSeconds(offsetTime.toEpochSecond)
  val clock: Clock =
    Clock.fixed(offsetTime.toInstant, offsetTime.toZonedDateTime.getZone)
  val timestampTolerance: FiniteDuration = 5.minutes
  val testSecret: String = "test-secret"
  val testSignatureInvalid: HmacSignature = HmacSignature("test-signature")

  val algorithm: String = "HmacSHA256"
  val secret: SecretKeySpec = new SecretKeySpec(testSecret.getBytes, algorithm)
  val testBody: ByteString = ByteString("test-body")

  def payload(timestamp: EpochSeconds, body: ByteString): String =
    s"v0:${timestamp.value}:${body.utf8String}"

  def expectedSignature(macBytes: Array[Byte]): HmacSignature = {
    HmacSignature(
      ByteString(
        s"v0=${DatatypeConverter.printHexBinary(macBytes).toLowerCase}"
      )
    )
  }
  def toEpochSeconds(timestamp: String): EpochSeconds = EpochSeconds(
    OffsetDateTime.parse(timestamp).toEpochSecond
  )

  def validSignature(
      body: ByteString,
      timestamp: EpochSeconds
  ): HmacSignature = {
    val testPayload: String = payload(timestamp, body)
    val mac: Mac = Mac.getInstance(algorithm)
    mac.init(secret)
    val signatureBytes: Array[Byte] = mac.doFinal(testPayload.getBytes)
    expectedSignature(signatureBytes)
  }

  "HmacSHA256SignatureVerify" should {

    "throw an exception for an incorrect signature" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val result = verifier.validate(clock)(timestampTolerance)(payload)(
        expectedSignature
      )(testSecret)(
        timestamp,
        testBody,
        testSignatureInvalid
      )
      result should matchPattern { case Failure(InvalidSignatureException) =>
      }
    }

    "return success for a valid signature" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val signature = validSignature(testBody, timestamp)
      val result =
        verifier.validate(clock)(timestampTolerance)(payload)(
          expectedSignature
        )(testSecret)(timestamp, testBody, signature)
      result should matchPattern { case Success(`testBody`) => }
    }

    "return success for a stale timestamp within tolerance" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val timestamp = "2017-09-15T13:49:30.526+05:30"
      val slightlyStaleTimestamp = toEpochSeconds(timestamp)
      val result = verifier.validate(clock)(timestampTolerance)(payload)(
        expectedSignature
      )(testSecret)(
        slightlyStaleTimestamp,
        testBody,
        validSignature(testBody, slightlyStaleTimestamp)
      )
      result should matchPattern { case Success(`testBody`) => }
    }

    "return failure for a stale timestamp" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val invalidTimestampStr = "2017-09-15T13:40:30.526+05:30"
      val staleTimestamp = toEpochSeconds(invalidTimestampStr)
      val result = verifier.validate(clock)(timestampTolerance)(payload)(
        expectedSignature
      )(testSecret)(
        staleTimestamp,
        testBody,
        validSignature(testBody, staleTimestamp)
      )
      result should matchPattern { case Failure(InvalidTimestampException) => }
    }

    "return failure for a timestamp in the future" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val invalidTimestampStr = "2017-09-15T13:58:30.526+05:30"
      val futureTimestamp = toEpochSeconds(invalidTimestampStr)
      val result = verifier.validate(clock)(timestampTolerance)(payload)(
        expectedSignature
      )(testSecret)(
        futureTimestamp,
        testBody,
        validSignature(testBody, futureTimestamp)
      )
      result should matchPattern { case Failure(InvalidTimestampException) => }
    }

  }

}
