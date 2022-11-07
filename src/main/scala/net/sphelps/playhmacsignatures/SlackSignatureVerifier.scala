package net.sphelps.playhmacsignatures

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration

import scala.util.{Failure, Success, Try}

case class InvalidSignatureException(expectedSignature: String)
    extends Exception(s"Invalid signature: expected $expectedSignature")

@ImplementedBy(classOf[SlackSignatureVerifier])
trait SignatureVerifierService {
  def validate(
      timestamp: String,
      body: String,
      signature: String
  ): Try[String]
}

@Singleton
class SlackSignatureVerifier @Inject() (protected val config: Configuration)
    extends SignatureVerifierService {

  val signingSecret: String = config.get[String]("slack.signingSecret")

  def validate(
      timestamp: String,
      body: String,
      signature: String
  ): Try[String] = {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    import javax.xml.bind.DatatypeConverter

    val secret = new SecretKeySpec(signingSecret.getBytes, "HmacSHA256")
    val payload = s"v0:$timestamp:$body"

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secret)

    val signatureBytes = mac.doFinal(payload.getBytes)
    val expectedSignature =
      s"v0=${DatatypeConverter.printHexBinary(signatureBytes).toLowerCase}"
    if (signature == expectedSignature) {
      Success(body)
    } else {
      Failure(InvalidSignatureException(expectedSignature))
    }
  }
}
