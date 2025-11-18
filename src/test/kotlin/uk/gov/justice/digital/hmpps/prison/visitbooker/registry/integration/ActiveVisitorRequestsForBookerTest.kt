package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.GET_ACTIVE_VISITOR_REQUESTS
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@DisplayName("Get active visitor requests for booker")
class ActiveVisitorRequestsForBookerTest : IntegrationTestBase() {

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")
  }

  @Test
  fun `when get active visitor requests by booker reference is called only REQUESTED status requests are returned`() {
    // Given
    val bookerReference = booker1.reference
    val request1 = createVisitorRequest(booker1.reference, "A1234", AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)
    // status not REQUESTED
    createVisitorRequest(booker1.reference, "A1234", AddVisitorToBookerPrisonerRequestDto("firstName2", "lastName2", LocalDate.now().minusYears(22)), status = VisitorRequestsStatus.APPROVED)
    // status not REQUESTED
    createVisitorRequest(booker1.reference, "A1234", AddVisitorToBookerPrisonerRequestDto("firstName3", "lastName3", LocalDate.now().minusYears(23)), status = VisitorRequestsStatus.REJECTED)
    val request4 = createVisitorRequest(booker1.reference, "A1233", AddVisitorToBookerPrisonerRequestDto("firstName4", "lastName4", LocalDate.now().minusYears(24)), status = VisitorRequestsStatus.REQUESTED)
    // same prisoner REQUESTED - but other booker
    createVisitorRequest(booker2.reference, "A1233", AddVisitorToBookerPrisonerRequestDto("firstName4", "lastName4", LocalDate.now().minusYears(24)), status = VisitorRequestsStatus.REQUESTED)

    // When
    val responseSpec = getActiveVisitorRequestsByBookerReference(webTestClient, bookerReference, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeVisitorRequests = getResults(returnResult)
    Assertions.assertThat(activeVisitorRequests.size).isEqualTo(2)
    assertActiveVisitorRequest(activeVisitorRequests[0], request1)
    assertActiveVisitorRequest(activeVisitorRequests[1], request4)
  }

  @Test
  fun `when get active visitor requests by booker reference is called and no requests are pending an empty list is returned`() {
    // Given
    val bookerReference = booker1.reference

    // status not REQUESTED
    createVisitorRequest(booker1.reference, "A1234", AddVisitorToBookerPrisonerRequestDto("firstName2", "lastName2", LocalDate.now().minusYears(22)), status = VisitorRequestsStatus.APPROVED)
    // status not REQUESTED
    createVisitorRequest(booker1.reference, "A1234", AddVisitorToBookerPrisonerRequestDto("firstName3", "lastName3", LocalDate.now().minusYears(23)), status = VisitorRequestsStatus.REJECTED)
    // for other booker
    createVisitorRequest(booker2.reference, "A1233", AddVisitorToBookerPrisonerRequestDto("firstName4", "lastName4", LocalDate.now().minusYears(24)), status = VisitorRequestsStatus.REQUESTED)

    // When
    val responseSpec = getActiveVisitorRequestsByBookerReference(webTestClient, bookerReference, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val activeVisitorRequests = getResults(returnResult)
    Assertions.assertThat(activeVisitorRequests.size).isEqualTo(0)
  }

  @Test
  fun `when get active visitor requests by booker reference is called and booker does not exist then NOT_FOUND status is returned`() {
    // Given
    val bookerReference = "booker-missing"

    // When
    val responseSpec = getActiveVisitorRequestsByBookerReference(webTestClient, bookerReference, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getActiveVisitorRequestsByBookerReference(webTestClient, booker1.reference, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerPrisonerVisitorRequestDto> = objectMapper.readValue(returnResult.returnResult().responseBody, Array<BookerPrisonerVisitorRequestDto>::class.java).toList()

  private fun assertActiveVisitorRequest(visitorRequestDto: BookerPrisonerVisitorRequestDto, visitorRequest: VisitorRequest) {
    Assertions.assertThat(visitorRequestDto.reference).isEqualTo(visitorRequest.reference)
    Assertions.assertThat(visitorRequestDto.bookerReference).isEqualTo(visitorRequest.bookerReference)
    Assertions.assertThat(visitorRequestDto.prisonerId).isEqualTo(visitorRequest.prisonerId)
    Assertions.assertThat(visitorRequestDto.firstName).isEqualTo(visitorRequest.firstName)
    Assertions.assertThat(visitorRequestDto.lastName).isEqualTo(visitorRequest.lastName)
    Assertions.assertThat(visitorRequestDto.dateOfBirth).isEqualTo(visitorRequest.dateOfBirth)
    Assertions.assertThat(visitorRequestDto.status).isEqualTo(visitorRequest.status)
  }

  fun getActiveVisitorRequestsByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.get().uri(GET_ACTIVE_VISITOR_REQUESTS.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .exchange()
}
