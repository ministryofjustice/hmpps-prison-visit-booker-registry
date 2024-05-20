package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.BOOKER_LINKED_PRISONER_VISITORS
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner

@DisplayName("Get permitted prisoner's permitted visitors for booker")
class PrisonersVisitorsByBookerReferenceTest : IntegrationTestBase() {
  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  private lateinit var permittedPrisoner1: PermittedPrisoner
  private lateinit var permittedPrisoner2: PermittedPrisoner

  private lateinit var visitor1: PermittedVisitorDetails
  private lateinit var visitor2: PermittedVisitorDetails
  private lateinit var visitor3: PermittedVisitorDetails
  private lateinit var visitor4: PermittedVisitorDetails

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has 2 permittedPrisoners associated but no permittedVisitors
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    val prisoner1Details = PermittedPrisonerDetails("AB123456", true)
    val prisoner2Details = PermittedPrisonerDetails("AB789012", true)

    val prisoners = createAssociatedPrisoners(
      booker1,
      listOf(prisoner1Details, prisoner2Details),
      visitors = listOf(),
    )

    createAssociatedPrisoners(
      booker2,
      listOf(prisoner1Details),
      visitors = listOf(),
    )

    visitor1 = PermittedVisitorDetails(12, true)
    visitor2 = PermittedVisitorDetails(34, true)
    visitor3 = PermittedVisitorDetails(56, true)
    visitor4 = PermittedVisitorDetails(78, false)

    permittedPrisoner1 = prisoners[0]
    permittedPrisoner2 = prisoners[1]

    createAssociatedPrisonersVisitors(
      permittedPrisoner1,
      listOf(visitor1, visitor2, visitor3, visitor4),
    )

    createAssociatedPrisonersVisitors(
      permittedPrisoner2,
      listOf(
        visitor3,
        visitor4,
      ),
    )
  }

  @Test
  fun `get visitors by valid reference returns all visitors associated with that prisoner if active param is null`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, permittedPrisoner1.prisonerId, null, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedVisitors = getResults(returnResult)

    Assertions.assertThat(associatedVisitors.size).isEqualTo(4)
    assertVisitorDetails(associatedVisitors[0], visitor1)
    assertVisitorDetails(associatedVisitors[1], visitor2)
    assertVisitorDetails(associatedVisitors[2], visitor3)
    assertVisitorDetails(associatedVisitors[3], visitor4)
  }

  @Test
  fun `get visitors by valid reference returns only active visitors associated with that prisoner if active param is true`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, permittedPrisoner1.prisonerId, true, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedVisitors = getResults(returnResult)
    Assertions.assertThat(associatedVisitors.size).isEqualTo(3)
    assertVisitorDetails(associatedVisitors[0], visitor1)
    assertVisitorDetails(associatedVisitors[1], visitor2)
    assertVisitorDetails(associatedVisitors[2], visitor3)
  }

  @Test
  fun `get visitors by valid reference returns only active visitors associated with that prisoner if active param is false`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, permittedPrisoner1.prisonerId, false, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedVisitors = getResults(returnResult)
    Assertions.assertThat(associatedVisitors.size).isEqualTo(1)
    assertVisitorDetails(associatedVisitors[0], visitor4)
  }

  @Test
  fun `get visitors by valid reference returns no visitors when none associated with that prisoner`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker2.reference, permittedPrisoner1.prisonerId, null, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, "invalid-reference", permittedPrisoner1.prisonerId, null, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when invalid prisoner Id then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, "invalid-prison-number", null, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getPrisonerVisitorsByBookerReference(webTestClient, booker1.reference, permittedPrisoner1.prisonerId, null, setAuthorisation(roles = listOf()))

    // Then
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<PermittedVisitorDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PermittedVisitorDto>::class.java).toList()
  }

  private fun assertVisitorDetails(visitor: PermittedVisitorDto, visitorDetails: PermittedVisitorDetails) {
    Assertions.assertThat(visitor.visitorId).isEqualTo(visitorDetails.visitorId)
    Assertions.assertThat(visitor.active).isEqualTo(visitorDetails.isActive)
  }

  fun getPrisonerVisitorsByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    active: Boolean? = null,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    var url = BOOKER_LINKED_PRISONER_VISITORS.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    active?.let {
      url += "?active=$active"
    }
    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}

class PermittedVisitorDetails(
  val visitorId: Long,
  val isActive: Boolean,
)
