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
    val signatureVerifyAction: SlackSignatureVerifyAction
)(implicit ec: ExecutionContext)
    extends BaseController
    with HMACSignatureHelpers {

  private val onSignatureValid =
    validateSignatureParseAndProcess(signatureVerifyAction)(Json.parse)(_)

  def test: Action[ByteString] = {
    onSignatureValid { body: JsValue =>
      Future {
        Ok((body \ "message").toString)
      }
    }
  }
}
