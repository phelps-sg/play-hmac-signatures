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
  def validate(
      payload: (Long, ByteString) => String
  )(expectedSignature: Array[Byte] => ByteString)(signingSecret: String)(
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
      expectedSignature: Array[Byte] => ByteString
  )(
      signingSecret: String
  )(
      timestamp: Long,
      body: ByteString,
      signature: ByteString
  ): Try[ByteString] = {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec

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
