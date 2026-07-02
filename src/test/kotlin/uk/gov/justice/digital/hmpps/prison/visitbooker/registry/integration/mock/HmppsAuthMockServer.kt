package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.createJsonResponseBuilder

class HmppsAuthExtension :
  BeforeAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val hmppsAuthApi = HmppsAuthMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsAuthApi.startIfNeeded()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsAuthApi.startIfNeeded()
    hmppsAuthApi.resetAll()
    hmppsAuthApi.stubGrantToken()
    hmppsAuthApi.stubGetUserDetails("created-user")
    hmppsAuthApi.stubGetUserDetails("updated-user")
    hmppsAuthApi.stubGetUserDetails("cancelled-user")
  }

  private fun HmppsAuthMockServer.startIfNeeded() {
    if (!isRunning) {
      start()
    }
  }
}

class HmppsAuthMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8090
  }

  fun stubGrantToken() {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """
              {
                "token_type": "bearer",
                "access_token": "atoken"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetUserDetails(userId: String, fullName: String? = "$userId-name") {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/auth/api/user/$userId")
        .willReturn(
          responseBuilder
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """
              {
                 "username": "$userId",
                 "name": "$fullName"
                }
              """.trimIndent(),
            ),
        ),
    )
  }
}
