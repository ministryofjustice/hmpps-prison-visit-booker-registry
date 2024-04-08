package uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.BasicContactDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.PrisonerBasicInfoDto

class VisitsOrchestrationMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8091) {
  fun stubGetBasicPrisonerDetails(prisonerIds: List<String>, basicPrisonerInfoList: List<PrisonerBasicInfoDto>?) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/prisoner/${prisonerIds.joinToString(",")}/basic-details")
        .willReturn(
          if (basicPrisonerInfoList == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(basicPrisonerInfoList))
          },
        ),
    )
  }

  fun stubBasicVisitorsBasicDetails(prisonerId: String, visitorIds: List<Long>, basicContactList: List<BasicContactDto>?) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/prisoner/$prisonerId/visitors/${visitorIds.joinToString(",")}/basic-details")
        .willReturn(
          if (basicContactList == null) {
            responseBuilder.withStatus(HttpStatus.NOT_FOUND.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(basicContactList))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String {
    return objectMapper.writer().writeValueAsString(obj)
  }

  private fun createJsonResponseBuilder(): ResponseDefinitionBuilder {
    return aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
  }
}
