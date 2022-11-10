package net.sphelps.playhmacsignatures

import com.google.inject.{ImplementedBy, Singleton}

import scala.util.{Failure, Success, Try}

case object InvalidSignatureException
    extends Exception(s"Invalid signature")

@ImplementedBy(classOf[HmacSHA256SignatureVerifier])
trait SignatureVerifierService {
  def validate(signingSecret: => String)(
      timestamp: String,
      body: String,
      signature: String
  ): Try[String]
}

@Singleton
class HmacSHA256SignatureVerifier extends SignatureVerifierService {

  val algorithm = "HmacSHA256"

  def validate(
      signingSecret: => String
  )(
      timestamp: String,
      body: String,
      signature: String
  ): Try[String] = {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    import javax.xml.bind.DatatypeConverter

    val secret = new SecretKeySpec(signingSecret.getBytes, algorithm)
    val payload = s"v0:$timestamp:$body"

    val mac = Mac.getInstance(algorithm)
    mac.init(secret)

    val signatureBytes = mac.doFinal(payload.getBytes)
    val expectedSignature =
      s"v0=${DatatypeConverter.printHexBinary(signatureBytes).toLowerCase}"
    if (signature == expectedSignature) {
      Success(body)
    } else {
      Failure(InvalidSignatureException)
    }
  }
}
