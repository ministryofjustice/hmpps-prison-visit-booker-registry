package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.GET_SINGLE_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@DisplayName("Get single visitor request - $GET_SINGLE_VISITOR_REQUEST")
class GetSingleVisitorRequestForBookerPrisonerByReferenceTest : IntegrationTestBase() {

  @Test
  fun `when get single request by is called, then request is returned`() {
    // Given
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)

    // When
    val responseSpec = callGetSingleVisitorRequest(webTestClient, request.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val singleRequest = getResults(returnResult)
    Assertions.assertThat(singleRequest.bookerReference).isEqualTo(booker.reference)
    Assertions.assertThat(singleRequest.prisonerId).isEqualTo(prisoner.prisonerId)
    Assertions.assertThat(singleRequest.reference).isEqualTo(request.reference)
    Assertions.assertThat(singleRequest.requestedOn).isEqualTo(LocalDate.now())
  }

  @Test
  fun `when get single visitor request is called and booker does not exist then NOT_FOUND status is returned`() {
    // Given
    val reference = "abc-def-ghi"

    // When
    val responseSpec = callGetSingleVisitorRequest(webTestClient, reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when get single visitor request is called and visitor request does not exist then NOT_FOUND status is returned`() {
    // Given
    val reference = "missingRef"

    // When
    val responseSpec = callGetSingleVisitorRequest(webTestClient, reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = callGetSingleVisitorRequest(webTestClient, "visitorRequestRef", setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonVisitorRequestDto = objectMapper.readValue(returnResult.returnResult().responseBody, PrisonVisitorRequestDto::class.java)

  fun callGetSingleVisitorRequest(
    webTestClient: WebTestClient,
    requestReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get()
    .uri(GET_SINGLE_VISITOR_REQUEST.replace("{requestReference}", requestReference))
    .headers(authHttpHeaders)
    .exchange()
}
