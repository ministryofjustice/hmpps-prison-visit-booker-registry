package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.createJsonResponseBuilder
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.MockUtils.Companion.getJsonString

class PrisonerContactRegistryMockServer : WireMockServer(8093) {
  fun stubGetPrisonerContacts(
    prisonerId: String,
    contactsList: List<PrisonerContactDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/v2/prisoners/$prisonerId/contacts/social?${getContactsQueryParams()}")
        .willReturn(
          if (contactsList == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(contactsList))
          },
        ),
    )
  }

  fun stubGetPrisonerContactViaRelationshipId(
    prisonerId: String,
    contactId: String,
    relationshipId: Long,
    contact: PrisonerContactDto?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = createJsonResponseBuilder()

    stubFor(
      get("/v2/prisoners/$prisonerId/contacts/$contactId/relationships/$relationshipId?withRestrictions=false")
        .willReturn(
          if (contact == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(contact))
          },
        ),
    )
  }

  private fun getContactsQueryParams(): String {
    val queryParams = kotlin.collections.ArrayList<String>()

    queryParams.add("hasDateOfBirth=false")
    queryParams.add("withRestrictions=false")

    return queryParams.joinToString("&")
  }
}
