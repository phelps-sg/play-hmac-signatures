/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

import akka.util.ByteString
import com.google.inject.{ImplementedBy, Singleton}

import scala.util.{Failure, Success, Try}

case object InvalidSignatureException extends Exception("Invalid signature")

@ImplementedBy(classOf[HmacSHA256SignatureVerifier])
trait SignatureVerifierService {
  def validate(payload: (Long, ByteString) => String)(signingSecret: String)(
      timestamp: Long,
      body: ByteString,
      signature: ByteString
  ): Try[ByteString]
}

@Singleton
class HmacSHA256SignatureVerifier extends SignatureVerifierService {

  val algorithm = "HmacSHA256"

  def validate(
      payload: (Long, ByteString) => String
  )(
      signingSecret: String
  )(
      timestamp: Long,
      body: ByteString,
      signature: ByteString
  ): Try[ByteString] = {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    import javax.xml.bind.DatatypeConverter

    val secret = new SecretKeySpec(signingSecret.getBytes, algorithm)

    val mac = Mac.getInstance(algorithm)
    mac.init(secret)

    val signatureBytes = mac.doFinal(payload(timestamp, body).getBytes)
    val expectedSignature = ByteString(
      s"v0=${DatatypeConverter.printHexBinary(signatureBytes).toLowerCase}"
    )
    if (signature == expectedSignature) {
      Success(body)
    } else {
      Failure(InvalidSignatureException)
    }
  }
}
