/*
 * Copyright (c) 2022 Steve Phelps
 */

import akka.util.ByteString
import com.mesonomics.playhmacsignatures.EpochSeconds
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpecLike

class ActionTests extends AnyWordSpecLike with should.Matchers {

  trait TestFixtures extends CommonFixtures {
    val timestamp: EpochSeconds = EpochSeconds(1000)
    val body: ByteString = ByteString("test-body")
  }

  "SlackSignatureVerifyAction" should {
    "compute the correct payload" in new TestFixtures {
      slackSignatureVerifyAction.payload(
        timestamp,
        body
      ) shouldBe "v0:1000:test-body"
    }
  }
}
