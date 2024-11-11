package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.apache.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto

class PrisonOffenderSearchMockServer : WireMockServer(8091) {

  companion object {
    val MAPPER: ObjectMapper = JsonMapper.builder()
      .addModule(JavaTimeModule())
      .build()
  }

  fun stubGetPrisoner(
    prisonerId: String,
    prisonerSearchResultDto: PrisonerDto?,
    httpStatus: Int = HttpStatus.SC_NOT_FOUND,
  ) {
    stubFor(
      get("/prisoner/$prisonerId")
        .willReturn(
          if (prisonerSearchResultDto == null) {
            aResponse().withStatus(httpStatus)
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

  private fun getJsonString(obj: Any): String {
    return MAPPER.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
  }
}
