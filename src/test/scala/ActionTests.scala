/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.util.ByteString
import com.mesonomics.playhmacsignatures.{EpochSeconds, HmacSignature}
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class ActionTests extends AnyWordSpecLike with should.Matchers {

  trait TestFixtures extends CommonFixtures {
    val timeSeconds: Long = 1000
    val timestamp: EpochSeconds = EpochSeconds(timeSeconds)
    val bodyStr: String = "test-body"
    val body: ByteString = ByteString(bodyStr)
    val signatureBytes: Array[Byte] = Array(0xff, 0x00, 0xf0).map(_.toByte)
  }

  "SlackSignatureVerifyAction" should {

    "compute the correct payload" in new TestFixtures {
      slackSignatureVerifyAction.payload(
        timestamp,
        body
      ) shouldBe s"v0:$timeSeconds:$bodyStr"
    }

    "compute the correct expectedSignature" in new TestFixtures {
      slackSignatureVerifyAction.expectedSignature(
        signatureBytes
      ) shouldEqual HmacSignature("v0=ff00f0")
    }
  }
}
