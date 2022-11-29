/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

import akka.util.ByteString
import com.mesonomics.playhmacsignatures.SignatureVerifyAction.SignedRequestByteStringValidator
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import play.api.{Configuration, Logging}
import play.core.parsers.FormUrlEncodedParser

import java.time.Clock
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** A Play Request containing an HMAC signature (as per
  * [[https://www.ietf.org/rfc/rfc2104.txt RFC 2104]]).
  *
  * @param validateSignature
  *   A call-back to validate the signature which returns a `Failure` if the
  *   signature is invalid
  * @param request
  *   The original request
  * @tparam A
  *   The body content type
  */
class SignedRequest[A](
    val validateSignature: ByteString => Try[ByteString],
    request: Request[A]
) extends WrappedRequest[A](request)

/** Helper functions for processing signed requests
  */
trait HMACSignatureHelpers {
  env: BaseControllerHelpers =>

  /** Helper function to create an asynchronous action which: validates a
    * signature on a `SignedRequest`, then parses the request body, and finally
    * processes the parsed data to return the final result. If the signature
    * headers are not supplied, or they are invalid, then the action will return
    * a 401 result in the future.
    *
    * @param signatureVerifyAction
    *   The action used to verify the signature headers
    * @param bodyParser
    *   A call-back to parse the raw (`Array[Byte]`) body
    * @param bodyProcessor
    *   A call-back to process the parsed body and return a future result
    * @param ec
    *   The execution context for the future
    * @tparam T
    *   The body content type expected by the body processor
    */
  def validateSignatureAsync[T](
      bodyParser: Array[Byte] => T
  )(
      bodyProcessor: T => Future[Result]
  )(implicit
      ec: ExecutionContext,
      signatureVerifyAction: SignatureVerifyAction
  ): Action[ByteString] =
    signatureVerifyAction.async(parse.byteString) { request =>
      request
        .validateSignatureAgainstBody(bodyParser)
        .map(bodyProcessor) match {
        case Success(result) => result
        case Failure(ex) =>
          Future {
            Unauthorized(ex.getMessage)
          }
      }
    }
}

object SignatureVerifyAction {

  /** Convenience function to parse url-encoded form data
    */
  def formUrlEncodedParser(rawBody: Array[Byte]): Map[String, Seq[String]] =
    FormUrlEncodedParser.parse(new String(rawBody))

  implicit class SignedRequestByteStringValidator(
      signedRequest: SignedRequest[ByteString]
  ) {

    /** Validate the signed request's signature against the body of the original
      * request represented as a UTF-8 string, and then parse the raw body.
      *
      * @param parser
      *   Call-back to parse the raw body
      * @tparam T
      *   The type of the parsed body
      * @return
      *   If the signature is valid then return `Success` with the parsed body,
      *   otherwise if the signature is invalid then result in `Failure`.
      */
    def validateSignatureAgainstBody[T](parser: Array[Byte] => T): Try[T] = {
      val rawBody: ByteString = signedRequest.body
      signedRequest.validateSignature(rawBody) map { _ =>
        parser(signedRequest.body.toArray)
      }
    }
  }
}

/** Abstract class for Play actions which verify HMAC signatures. Sub-classes
  * should override the `headersTimestamp` and `headersSignature` members with
  * the header keys that contain the timestamp and signature respectively.
  * Inject a mock `SignatureVerifierService` for unit testing.
  *
  * @param parser
  *   The body parser
  * @param signatureVerifierService
  *   The service used to validate the signature against the body
  * @param ec
  *   The execution context
  */
abstract class SignatureVerifyAction(
    val parser: BodyParsers.Default,
    val config: Configuration,
    signatureVerifierService: SignatureVerifierService,
    protected val clock: Clock = Clock.systemUTC(),
    protected val timestampTolerance: Duration = 5.minutes
)(implicit ec: ExecutionContext)
    extends ActionBuilder[SignedRequest, AnyContent]
    with ActionRefiner[Request, SignedRequest]
    with Logging {

  val headerKeyTimestamp: String
  val headerKeySignature: String
  val signingSecretConfigKey: String
  def payload(timestamp: EpochSeconds, body: ByteString): String
  def expectedSignature(macBytes: Array[Byte]): HmacSignature

  protected val validate
      : (EpochSeconds, ByteString, HmacSignature) => Try[ByteString] =
    signatureVerifierService.validate(clock)(timestampTolerance)(payload)(
      expectedSignature
    )(
      config.get[String](signingSecretConfigKey)
    )(_, _, _)

  protected def getTimestamp[A](request: Request[A]): Option[EpochSeconds] = {
    import Utils.LongStringScala212
    for {
      timestampStr <- request.headers.get(headerKeyTimestamp)
      t <- timestampStr.toLongOpt
    } yield EpochSeconds(t)
  }

  protected def getSignature[A](request: Request[A]): Option[HmacSignature] =
    request.headers.get(headerKeySignature) map { str =>
      HmacSignature(ByteString(str))
    }

  override protected def executionContext: ExecutionContext = ec

  override protected def refine[A](
      request: Request[A]
  ): Future[Either[Result, SignedRequest[A]]] = {
    val timestamp = getTimestamp(request)
    val signature = getSignature(request)
    (timestamp, signature) match {
      case (Some(timestamp), Some(signature)) =>
        Future.successful {
          val callback =
            (body: ByteString) => validate(timestamp, body, signature)
          Right(new SignedRequest[A](callback, request))
        }
      case _ =>
        Future {
          Left(Unauthorized("Invalid signature headers"))
        }
    }
  }
}
