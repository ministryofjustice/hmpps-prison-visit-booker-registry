package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.BOOKER_ENDPOINT_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@DisplayName("Clear booker by reference - $BOOKER_ENDPOINT_PATH")
class ClearBookerByReferenceTest : IntegrationTestBase() {

  private lateinit var booker1: Booker

  private lateinit var prisoner1: PermittedPrisonerTestObject
  private lateinit var prisoner2: PermittedPrisonerTestObject

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    prisoner1 = PermittedPrisonerTestObject("AB123456", PRISON_CODE)
    prisoner2 = PermittedPrisonerTestObject("AB789012", PRISON_CODE)

    createAssociatedPrisoners(
      booker1,
      listOf(prisoner1, prisoner2),
    )
  }

  @Test
  fun `clear booker by reference`() {
    // When
    val responseSpec = clearBookerByReference(webTestClient, booker1.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val booker = getResults(returnResult)
    assertThat(booker.reference).isEqualTo(booker1.reference)

    val bookers = bookerRepository.findAll()
    assertThat(bookers.size).isEqualTo(1)

    val prisoners = permittedPrisonerRepository.findAll()
    assertThat(prisoners).isEmpty()
  }

  @Test
  fun `when visitor requests exist, and clear booker by reference is called, then visitor requests for that booker and prisoner are deleted`() {
    // When
    createVisitorRequest(booker1.reference, prisoner1.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName", "lastName", LocalDate.now().minusYears(21)), VisitorRequestsStatus.REQUESTED)

    val responseSpec = clearBookerByReference(webTestClient, booker1.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val booker = getResults(returnResult)
    assertThat(booker.reference).isEqualTo(booker1.reference)

    val bookers = bookerRepository.findAll()
    assertThat(bookers.size).isEqualTo(1)

    val prisoners = permittedPrisonerRepository.findAll()
    assertThat(prisoners).isEmpty()

    val visitorRequests = visitorRequestsRepository.findAll()
    assertThat(visitorRequests).isEmpty()
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = clearBookerByReference(webTestClient, "invalid-reference", bookerConfigServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Booker not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Booker for reference : invalid-reference not found")
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = clearBookerByReference(webTestClient, booker1.reference, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): BookerDto = objectMapper.readValue(returnResult.returnResult().responseBody, BookerDto::class.java)

  fun clearBookerByReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = BOOKER_ENDPOINT_PATH.replace("{bookerReference}", bookerReference)
    return webTestClient.delete().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
