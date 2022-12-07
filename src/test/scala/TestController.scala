/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.util.ByteString
import com.mesonomics.playhmacsignatures.{
  HMACSignatureHelpers,
  SlackSignatureVerifyAction
}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BaseController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class TestController(
    val controllerComponents: ControllerComponents,
    implicit val signatureVerifyAction: SlackSignatureVerifyAction
)(implicit ec: ExecutionContext)
    extends BaseController
    with HMACSignatureHelpers {

  private val onSignatureValid = validateSignatureAsync(Json.parse)(_)

  def test: Action[ByteString] =
    onSignatureValid { body: JsValue =>
      Future.successful {
        val message = body("message")
        Ok(message)
      }
    }
}
