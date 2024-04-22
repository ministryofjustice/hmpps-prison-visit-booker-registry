package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.util.function.Tuples
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@DisplayName("Get prisoners for booker")
class PrisonersByBookerReferenceTest : IntegrationTestBase() {
  private lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  private lateinit var prisoner1: PrisonerDetails
  private lateinit var prisoner2: PrisonerDetails
  private lateinit var prisoner3: PrisonerDetails

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI"))

    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has no prisoners associated
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    prisoner1 = PrisonerDetails("AB123456", true)
    prisoner2 = PrisonerDetails("AB789012", true)

    // inactive prisoner
    prisoner3 = PrisonerDetails("AB345678", false)

    createAssociatedPrisoners(
      booker1,
      listOf(
        Tuples.of(prisoner1.prisonerNumber, prisoner1.isActive),
        Tuples.of(prisoner2.prisonerNumber, prisoner2.isActive),
        Tuples.of(prisoner3.prisonerNumber, prisoner3.isActive),
      ),
    )
  }

  fun getPrisonersByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    active: Boolean? = null,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    var url = "/public/booker/$bookerReference/prisoners"
    if (active != null) {
      url += "?active=$active"
    }
    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `get prisoners by valid reference returns all prisoners associated with that booker if active parameter is null`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, null, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(3)
    assertPrisonerDetails(associatedPrisoners[0], prisoner1)
    assertPrisonerDetails(associatedPrisoners[1], prisoner2)
    assertPrisonerDetails(associatedPrisoners[2], prisoner3)
  }

  @Test
  fun `get prisoners by valid reference returns only acitve prisoners associated with that booker if active parameter is true`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, true, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(2)
    assertPrisonerDetails(associatedPrisoners[0], prisoner1)
    assertPrisonerDetails(associatedPrisoners[1], prisoner2)
  }

  @Test
  fun `get prisoners by valid reference returns only inacitve prisoners associated with that booker if active parameter is false`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, false, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(1)
    assertPrisonerDetails(associatedPrisoners[0], prisoner3)
  }

  @Test
  fun `get prisoners by valid reference returns no prisoners when none associated with that booker`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker2.reference, null, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, "invalid-reference", null, roleVisitSchedulerHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, null, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerPrisonersDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<BookerPrisonersDto>::class.java).toList()
  }

  private fun assertPrisonerDetails(bookerPrisoner: BookerPrisonersDto, prisonerDetail: PrisonerDetails) {
    Assertions.assertThat(bookerPrisoner.prisonerNumber).isEqualTo(prisonerDetail.prisonerNumber)
    Assertions.assertThat(bookerPrisoner.active).isEqualTo(prisonerDetail.isActive)
  }
}

class PrisonerDetails(
  val prisonerNumber: String,
  val isActive: Boolean,
)
