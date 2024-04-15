package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.util.function.Tuples
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AssociatedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@DisplayName("Get prisoners for booker")
class PrisonersByBookerReferenceTest : IntegrationTestBase() {
  private lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  private lateinit var booker1: Booker

  private lateinit var booker2: Booker

  private lateinit var prisoner1: PrisonerDetails

  private lateinit var prisoner2: PrisonerDetails

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI"))

    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has no prisoners associated
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    prisoner1 = PrisonerDetails("AB123456", "PrisonerOne", "NumberUno", true)
    prisoner2 = PrisonerDetails("AB789012", "PrisonerTwo", "NumberTwo", false)

    createAssociatedPrisoners(
      booker1,
      listOf(
        Tuples.of(prisoner1.prisonerNumber, prisoner1.isActive),
        Tuples.of(prisoner2.prisonerNumber, prisoner2.isActive),
      ),
    )
  }

  fun getPrisonersByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/public/booker/$bookerReference/prisoners")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `get prisoners by valid reference returns all prisoners associated with that booker`() {
    // Given
    visitsOrchestrationMockServer.stubGetBasicPrisonerDetails(
      listOf(
        prisoner1.prisonerNumber,
        prisoner2.prisonerNumber,
      ),
      listOf(
        with(prisoner1) { PrisonerBasicInfoDto(this.prisonerNumber, this.firstName, this.lastName) },
        with(prisoner2) { PrisonerBasicInfoDto(this.prisonerNumber, this.firstName, this.lastName) },
      ),
    )

    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(2)
    assertPrisonerDetails(associatedPrisoners[0], prisoner1)
    assertPrisonerDetails(associatedPrisoners[1], prisoner2)
  }

  @Test
  fun `get prisoners by valid reference returns no prisoners when none associated with that booker`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker2.reference, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `get prisoners by valid reference returns UNKNOWN if the orchestration service returns a 404`() {
    // Given
    visitsOrchestrationMockServer.stubGetBasicPrisonerDetails(
      listOf(
        prisoner1.prisonerNumber,
        prisoner2.prisonerNumber,
      ),
      null,
    )

    // When
    val responseSpec =
      getPrisonersByBookerReference(webTestClient, booker1.reference, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(2)
    assertPrisonerDetailsWhenUnknown(associatedPrisoners[0], prisoner1)
    assertPrisonerDetailsWhenUnknown(associatedPrisoners[1], prisoner2)
  }

  @Test
  fun `get prisoners by valid reference returns UNKNOWN if the orchestration service returns empty response`() {
    // Given
    visitsOrchestrationMockServer.stubGetBasicPrisonerDetails(
      listOf(
        prisoner1.prisonerNumber,
        prisoner2.prisonerNumber,
      ),
      listOf(),
    )

    // When
    val responseSpec =
      getPrisonersByBookerReference(webTestClient, booker1.reference, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(2)
    assertPrisonerDetailsWhenUnknown(associatedPrisoners[0], prisoner1)
    assertPrisonerDetailsWhenUnknown(associatedPrisoners[1], prisoner2)
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, "invalid-reference", roleVisitSchedulerHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<AssociatedPrisonerDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<AssociatedPrisonerDto>::class.java).toList()
  }

  private fun assertPrisonerDetails(associatedPrisoner: AssociatedPrisonerDto, prisonerDetail: PrisonerDetails) {
    Assertions.assertThat(associatedPrisoner.prisonerNumber).isEqualTo(prisonerDetail.prisonerNumber)
    Assertions.assertThat(associatedPrisoner.firstName).isEqualTo(prisonerDetail.firstName)
    Assertions.assertThat(associatedPrisoner.lastName).isEqualTo(prisonerDetail.lastName)
    Assertions.assertThat(associatedPrisoner.isActive).isEqualTo(prisonerDetail.isActive)
  }

  private fun assertPrisonerDetailsWhenUnknown(associatedPrisoner: AssociatedPrisonerDto, prisonerDetail: PrisonerDetails) {
    Assertions.assertThat(associatedPrisoner.prisonerNumber).isEqualTo(prisonerDetail.prisonerNumber)
    Assertions.assertThat(associatedPrisoner.firstName).isEqualTo("NOT_KNOWN")
    Assertions.assertThat(associatedPrisoner.lastName).isEqualTo("NOT_KNOWN")
    Assertions.assertThat(associatedPrisoner.isActive).isEqualTo(prisonerDetail.isActive)
  }
}

class PrisonerDetails(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  val isActive: Boolean,
)
