package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.apache.http.HttpStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.getJsonString

class VisitSchedulerMockServer : WireMockServer(8092) {
  fun stubGetSupportedPublicPrisons(supportedPrisonsList: List<String>?, httpStatus: Int = HttpStatus.SC_NOT_FOUND) {
    stubFor(
      get("/config/prisons/user-type/PUBLIC/supported")
        .willReturn(
          if (supportedPrisonsList == null) {
            createJsonResponseBuilder()
              .withStatus(httpStatus)
          } else {
            createJsonResponseBuilder()
              .withStatus(HttpStatus.SC_OK)
              .withBody(getJsonString(supportedPrisonsList))
          },
        ),
    )
  }
}
