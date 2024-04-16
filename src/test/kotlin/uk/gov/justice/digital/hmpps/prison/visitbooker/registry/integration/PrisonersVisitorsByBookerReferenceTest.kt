package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.util.function.Tuples
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration.BasicContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner

@DisplayName("Get prisoner's visitors for booker")
class PrisonersVisitorsByBookerReferenceTest : IntegrationTestBase() {
  private lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  private lateinit var prisoner1: BookerPrisoner
  private lateinit var prisoner2: BookerPrisoner

  private lateinit var visitor1: PrisonersVisitorDetails
  private lateinit var visitor2: PrisonersVisitorDetails
  private lateinit var visitor3: PrisonersVisitorDetails
  private lateinit var visitor4: PrisonersVisitorDetails

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI"))

    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has 2 prisoners associated but no visitors
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    val prisoner1Details = PrisonerDetails("AB123456", "PrisonerOne", "NumberUno", true)
    val prisoner2Details = PrisonerDetails("AB789012", "PrisonerTwo", "NumberTwo", true)

    val prisoners = createAssociatedPrisoners(
      booker1,
      listOf(
        Tuples.of(prisoner1Details.prisonerNumber, prisoner1Details.isActive),
        Tuples.of(prisoner2Details.prisonerNumber, prisoner2Details.isActive),
      ),
    )

    createAssociatedPrisoners(
      booker2,
      listOf(
        Tuples.of(prisoner1Details.prisonerNumber, prisoner1Details.isActive),
      ),
    )

    visitor1 = PrisonersVisitorDetails(12, "VisitorOne", null, "LastNameOne", true)
    visitor2 = PrisonersVisitorDetails(34, "VisitorTwo", null, "LastNameTwo", true)
    visitor3 = PrisonersVisitorDetails(56, "VisitorThree", null, "LastNameThree", true)
    visitor4 = PrisonersVisitorDetails(78, "VisitorFour", null, "LastNameFour", false)

    prisoner1 = prisoners[0]
    prisoner2 = prisoners[1]

    createAssociatedPrisonersVisitors(
      prisoner1,
      listOf(
        Tuples.of(visitor1.personId, visitor1.isActive),
        Tuples.of(visitor2.personId, visitor2.isActive),
        Tuples.of(visitor3.personId, visitor3.isActive),
        Tuples.of(visitor4.personId, visitor4.isActive),
      ),
    )

    createAssociatedPrisonersVisitors(
      prisoner2,
      listOf(
        Tuples.of(visitor3.personId, visitor3.isActive),
        Tuples.of(visitor4.personId, visitor4.isActive),
      ),
    )
  }

  fun getPrisonerVisitorsByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    return webTestClient.get().uri("/public/booker/$bookerReference/prisoners/$prisonerId/visitors")
      .headers(authHttpHeaders)
      .exchange()
  }

  @Test
  fun `get visitors by valid reference returns only active visitors associated with that prisoner`() {
    // Given
    visitsOrchestrationMockServer.stubBasicVisitorsBasicDetails(
      prisoner1.prisonNumber,
      listOf(
        visitor1.personId,
        visitor2.personId,
        visitor3.personId,
      ),
      listOf(
        with(visitor1) { BasicContactDto(this.personId, this.firstName, this.middleName, this.lastName) },
        with(visitor2) { BasicContactDto(this.personId, this.firstName, this.middleName, this.lastName) },
        with(visitor3) { BasicContactDto(this.personId, this.firstName, this.middleName, this.lastName) },
      ),
    )

    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, prisoner1.prisonNumber, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedVisitors = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedVisitors.size).isEqualTo(3)
    assertVisitorDetails(associatedVisitors[0], visitor1)
    assertVisitorDetails(associatedVisitors[1], visitor2)
    assertVisitorDetails(associatedVisitors[2], visitor3)
  }

  @Test
  fun `get visitors by valid reference returns only details for prisoners whose details were returned`() {
    // Given
    visitsOrchestrationMockServer.stubBasicVisitorsBasicDetails(
      prisoner1.prisonNumber,
      listOf(
        visitor1.personId,
        visitor2.personId,
        visitor3.personId,
      ),
      listOf(
        with(visitor1) { BasicContactDto(this.personId, this.firstName, this.middleName, this.lastName) },
        with(visitor3) { BasicContactDto(this.personId, this.firstName, this.middleName, this.lastName) },
      ),
    )

    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, prisoner1.prisonNumber, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedVisitors = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedVisitors.size).isEqualTo(2)
    assertVisitorDetails(associatedVisitors[0], visitor1)
    assertVisitorDetails(associatedVisitors[1], visitor3)
  }

  @Test
  fun `get visitors by valid reference returns no visitors when none associated with that prisoner`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker2.reference, prisoner1.prisonNumber, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `get visitors by valid reference returns no records if the orchestration service returns a 404`() {
    // Given
    visitsOrchestrationMockServer.stubBasicVisitorsBasicDetails(
      prisoner1.prisonNumber,
      listOf(
        visitor1.personId,
        visitor2.personId,
        visitor3.personId,
      ),
      null,
    )

    // When
    val responseSpec =
      getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, prisoner1.prisonNumber, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `get visitors by valid reference returns no records if the orchestration service returns empty response`() {
    // Given
    visitsOrchestrationMockServer.stubBasicVisitorsBasicDetails(
      prisoner1.prisonNumber,
      listOf(
        visitor1.personId,
        visitor2.personId,
        visitor3.personId,
      ),
      emptyList(),
    )

    // When
    val responseSpec =
      getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, prisoner1.prisonNumber, roleVisitSchedulerHttpHeaders)
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    // Then
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, "invalid-reference", prisoner1.prisonNumber, roleVisitSchedulerHttpHeaders)

    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when invalid prisoner Id then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, "invalid-prison-number", roleVisitSchedulerHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, prisoner1.prisonNumber, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BasicContactDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<BasicContactDto>::class.java).toList()
  }

  private fun assertVisitorDetails(visitor: BasicContactDto, visitorDetails: PrisonersVisitorDetails) {
    Assertions.assertThat(visitor.personId).isEqualTo(visitorDetails.personId)
    Assertions.assertThat(visitor.firstName).isEqualTo(visitorDetails.firstName)
    Assertions.assertThat(visitor.lastName).isEqualTo(visitorDetails.lastName)
  }
}

class PrisonersVisitorDetails(
  val personId: Long,
  val firstName: String,
  val middleName: String? = null,
  val lastName: String,
  val isActive: Boolean,
)
