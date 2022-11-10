import net.sphelps.playhmacsignatures.{
  HmacSHA256SignatureVerifier,
  InvalidSignatureException
}
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import scala.util.{Failure, Success}

class ServiceTests extends AnyWordSpecLike with should.Matchers {

  val timestampStr: String = "02/01/2018 - 13:45:30 +0000"

  "HmacSHA256SignatureVerify" should {
    "throw an exception for an incorrect signature" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val testSecret = "test-secret"
      val testSignature = "test-signature"
      val testBody = "test-body"
      val result = verifier.validate(testSecret)(
        timestampStr,
        testBody,
        testSignature
      )
      result should matchPattern { case Failure(InvalidSignatureException(_)) =>
      }
    }

    "return success for a valid signature" in {
      val verifier = new HmacSHA256SignatureVerifier()
      val algorithm = "HmacSHA256"
      val testSecret = "test-secret"
      val testBody = "test-body"
      val secret = new SecretKeySpec(testSecret.getBytes, algorithm)
      val payload = s"v0:$timestampStr:$testBody"
      val mac = Mac.getInstance(algorithm)
      mac.init(secret)
      val signatureBytes = mac.doFinal(payload.getBytes)
      val signature =
        f"v0=${DatatypeConverter.printHexBinary(signatureBytes).toLowerCase}"
      val result =
        verifier.validate(testSecret)(timestampStr, testBody, signature)
      result should matchPattern { case Success(testBody) =>
      }
    }
  }

}
