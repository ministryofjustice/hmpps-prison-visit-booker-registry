package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.getJsonString

class PrisonOffenderSearchMockServer : WireMockServer(8091) {
  fun stubGetPrisoner(
    prisonerId: String,
    prisonerSearchResultDto: PrisonerDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    stubFor(
      get("/prisoner/$prisonerId")
        .willReturn(
          if (prisonerSearchResultDto == null) {
            aResponse().withStatus(httpStatus.value())
          } else {
            aResponse()
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withStatus(200)
              .withBody(
                getJsonString(prisonerSearchResultDto),
              )
          },
        ),
    )
  }
}
