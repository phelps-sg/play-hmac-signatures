/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.util.ByteString
import com.mesonomics.playhmacsignatures.{
  EpochSeconds,
  HMACSignatureHelpers,
  HmacSignature,
  SlackSignatureVerifyAction
}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents}

import javax.xml.bind.DatatypeConverter
import scala.concurrent.{ExecutionContext, Future}

object TestController {
  def payload(timestamp: EpochSeconds, body: ByteString): String =
    s"v0:$timestamp:${body.utf8String}"
  def expectedSignature(macBytes: Array[Byte]): HmacSignature = {
    HmacSignature(
      ByteString(
        s"v0=${DatatypeConverter.printHexBinary(macBytes).toLowerCase}"
      )
    )
  }
}

class TestController(
    val controllerComponents: ControllerComponents,
    implicit val signatureVerifyAction: SlackSignatureVerifyAction
)(implicit ec: ExecutionContext)
    extends BaseController
    with HMACSignatureHelpers {

  private val onSignatureValid = validateSignatureAsync(Json.parse)(_)

  def test: Action[ByteString] =
    onSignatureValid { body: JsValue =>
      Future {
        val message = body("message")
        Ok(message)
      }
    }
}
