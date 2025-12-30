package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import java.time.LocalDate

@Transactional(propagation = Propagation.SUPPORTS)
@DisplayName("GET count of active visitor requests for booker - $GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE")
class GetCountVisitorRequestsForPrisonTest : IntegrationTestBase() {

  private val prisonCode = "HEI"

  @Test
  fun `when get visitor requests count by prison code is called, then the count is returned for only that prison`() {
    // Given

    val booker = createBooker("abc-def-ghi", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode = prisonCode)
    val otherPrisoner = createPrisoner(booker, "BB123456", prisonCode = "XYZ")

    createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )
    createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("firstName2", "lastName2", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.APPROVED,
    )
    createVisitorRequest(
      booker.reference,
      otherPrisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )

    // When
    val responseSpec = getCountVisitorRequestsByPrisonCode(webTestClient, prisonCode, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val responseDto = getResults(returnResult)
    Assertions.assertThat(responseDto.count).isEqualTo(1)
  }

  @Test
  fun `when get visitor requests count by prison code is called, but no requests are found, then count is 0`() {
    // Given
    // When
    val responseSpec = getCountVisitorRequestsByPrisonCode(webTestClient, prisonCode, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val responseDto = getResults(returnResult)
    Assertions.assertThat(responseDto.count).isEqualTo(0)
  }

  @Test
  fun `when prisoner has booker in multiple prisons then count is returned only for prison where the requesting booker has registered prisoner`() {
    // Given
    val otherPrisonCode = "XYZ"
    val prisonerId = "AA123456"

    // prisoner AA123456 has 2 bookers
    // booker1 has registered prisoner against HEI
    val booker1 = createBooker("abc-def-ghi", "test@test.com")
    val prisoner = createPrisoner(booker1, prisonerId, prisonCode = prisonCode)

    // booker2 has registered same prisoner against XYZ
    val booker2 = createBooker("ccc-def-ghi", "test1@example.com")
    createPrisoner(booker2, prisonerId, prisonCode = otherPrisonCode)

    createVisitorRequest(
      booker1.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )
    createVisitorRequest(
      booker1.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("firstName2", "lastName2", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.APPROVED,
    )

    // When
    // get count by HEI should return 1
    var responseSpec = getCountVisitorRequestsByPrisonCode(webTestClient, prisonCode, orchestrationServiceRoleHttpHeaders)

    // Then
    var returnResult = responseSpec.expectStatus().isOk.expectBody()
    var responseDto = getResults(returnResult)
    Assertions.assertThat(responseDto.count).isEqualTo(1)

    // When
    // get count by XYZ should return 0
    responseSpec = getCountVisitorRequestsByPrisonCode(webTestClient, otherPrisonCode, orchestrationServiceRoleHttpHeaders)

    // Then
    returnResult = responseSpec.expectStatus().isOk.expectBody()
    responseDto = getResults(returnResult)
    Assertions.assertThat(responseDto.count).isEqualTo(0)
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getCountVisitorRequestsByPrisonCode(webTestClient, prisonCode, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): VisitorRequestsCountByPrisonCodeDto = objectMapper.readValue(returnResult.returnResult().responseBody, VisitorRequestsCountByPrisonCodeDto::class.java)

  fun getCountVisitorRequestsByPrisonCode(
    webTestClient: WebTestClient,
    prisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE.replace("{prisonCode}", prisonCode))
    .headers(authHttpHeaders)
    .exchange()
}
