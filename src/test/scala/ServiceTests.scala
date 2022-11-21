/*
 * Copyright (c) 2022 Steve Phelps
 */

import TestController.{expectedSignature, payload}
import akka.util.ByteString
import com.mesonomics.playhmacsignatures.{
  HmacSHA256SignatureVerifier,
  InvalidSignatureException
}
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import scala.util.{Failure, Success}

class ServiceTests extends AnyWordSpecLike with should.Matchers {

  val timestampStr: String = "2017-09-15T13:50:30.526+05:30"
  val timestamp: Long = OffsetDateTime.parse(timestampStr).toEpochSecond

  "HmacSHA256SignatureVerify" should {
    "throw an exception for an incorrect signature" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val testSecret = "test-secret"
      val testSignature = ByteString("test-signature")
      val testBody = ByteString("test-body")
      val result = verifier.validate(payload)(expectedSignature)(testSecret)(
        timestamp,
        testBody,
        testSignature
      )
      result should matchPattern { case Failure(InvalidSignatureException) =>
      }
    }

    "return success for a valid signature" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val algorithm = "HmacSHA256"
      val testSecret = "test-secret"
      val testBody = ByteString("test-body")
      val secret = new SecretKeySpec(testSecret.getBytes, algorithm)
      val testPayload = s"v0:$timestamp:${testBody.utf8String}"
      val mac = Mac.getInstance(algorithm)
      mac.init(secret)
      val signatureBytes = mac.doFinal(testPayload.getBytes)
      val signature = ByteString(
        f"v0=${DatatypeConverter.printHexBinary(signatureBytes).toLowerCase}"
      )
      val result =
        verifier.validate(payload)(expectedSignature)(testSecret)(
          timestamp,
          testBody,
          signature
        )
      result should matchPattern { case Success(`testBody`) => }
    }
  }

}
