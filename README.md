# play-hmac-signatures

[![GitHub Workflow Status](https://github.com/phelps-sg/play-hmac-signatures/actions/workflows/continouos-integration.yml/badge.svg)](https://github.com/phelps-sg/play-hmac-signatures/actions/workflows/continouos-integration.yml)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/phelps-sg/play-hmac-signatures)
![GitHub](https://img.shields.io/github/license/phelps-sg/play-hmac-signatures?color=blue)

## Installation

Add the following to `build.sbt`

~~~scala
libraryDependencies += "com.mesonomics" %% "play-hmac-signatures" % "0.6.1"
~~~

## Example usage

To validate an HMAC signature in a play controller mixin the `HMACSignatureHelpers` trait.

The `test` action in the example controller below will echo back the message only if the request is correctly signed.  On the other hand, if the signature is invalid it will return a 401 status.

~~~scala
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
~~~

[`SlackSignatureVerifyAction`](https://github.com/phelps-sg/play-hmac-signatures/blob/main/src/main/scala/com/mesonomics/playhmacsignatures/SlackSignatureVerifyAction.scala) looks for the following headers:

~~~
X-Slack-Request-Timestamp
X-Slack-Signature
~~~

and the signing secret is taken from the following configuration key:

~~~
slack.signingSecret
~~~~

To use different headers and/or configuration key, subclass `SignatureVerifyAction` and override abstract members.
