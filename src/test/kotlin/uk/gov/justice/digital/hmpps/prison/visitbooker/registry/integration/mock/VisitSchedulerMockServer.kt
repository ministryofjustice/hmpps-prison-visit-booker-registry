package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.getJsonString

class VisitSchedulerMockServer : WireMockServer(8092) {
  fun stubGetSupportedPublicPrisons(supportedPrisonsList: List<String>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    stubFor(
      get("/config/prisons/user-type/PUBLIC/supported")
        .willReturn(
          if (supportedPrisonsList == null) {
            createJsonResponseBuilder()
              .withStatus(httpStatus.value())
          } else {
            createJsonResponseBuilder()
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(supportedPrisonsList))
          },
        ),
    )
  }
}
