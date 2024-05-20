package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.BOOKER_LINKED_PRISONERS
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import java.util.*

@Transactional(propagation = SUPPORTS)
@DisplayName("Get permittedPrisoners for booker")
class PrisonersByBookerReferenceTest : IntegrationTestBase() {

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  private lateinit var prisoner1: PermittedPrisonerDetails
  private lateinit var prisoner2: PermittedPrisonerDetails
  private lateinit var prisoner3: PermittedPrisonerDetails

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has no permittedPrisoners associated
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    prisoner1 = PermittedPrisonerDetails("AB123456", true)
    prisoner2 = PermittedPrisonerDetails("AB789012", true)

    // inactive permittedPrisoner
    prisoner3 = PermittedPrisonerDetails("AB345678", false)

    createAssociatedPrisoners(
      booker1,
      listOf(prisoner1, prisoner2, prisoner3),
    )
  }

  @Test
  fun `get prisoners by valid reference returns all prisoners associated with that booker if active parameter is null`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, null, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(3)
    assertPrisonerDetails(associatedPrisoners[0], prisoner1)
    assertPrisonerDetails(associatedPrisoners[1], prisoner2)
    assertPrisonerDetails(associatedPrisoners[2], prisoner3)
  }

  @Test
  fun `get prisoners by valid reference returns only acitve prisoners associated with that booker if active parameter is true`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, true, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)

    Assertions.assertThat(associatedPrisoners.size).isEqualTo(2)
    assertPrisonerDetails(associatedPrisoners[0], prisoner1)
    assertPrisonerDetails(associatedPrisoners[1], prisoner2)
  }

  @Test
  fun `get prisoners by valid reference returns only inacitve prisoners associated with that booker if active parameter is false`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, false, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(1)
    assertPrisonerDetails(associatedPrisoners[0], prisoner3)
  }

  @Test
  fun `get prisoners by valid reference returns no prisoners when none associated with that booker`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker2.reference, null, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val associatedPrisoners = getResults(returnResult)
    Assertions.assertThat(associatedPrisoners.size).isEqualTo(0)
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, "invalid-reference", null, orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getPrisonersByBookerReference(webTestClient, booker1.reference, null, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<PermittedPrisonerDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PermittedPrisonerDto>::class.java).toList()
  }

  private fun assertPrisonerDetails(prisoner: PermittedPrisonerDto, prisonerDetail: PermittedPrisonerDetails) {
    Assertions.assertThat(prisoner.prisonerId).isEqualTo(prisonerDetail.prisonerId)
    Assertions.assertThat(prisoner.active).isEqualTo(prisonerDetail.isActive)
    Assertions.assertThat(prisoner.permittedVisitors).hasSize(1)
    Assertions.assertThat(prisoner.permittedVisitors[0].visitorId).isEqualTo(1)
    Assertions.assertThat(prisoner.permittedVisitors[0].active).isTrue()
  }

  fun getPrisonersByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    active: Boolean? = null,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    var url = BOOKER_LINKED_PRISONERS.replace("{bookerReference}", bookerReference)
    active?.let {
      url += "?active=$active"
    }
    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}

class PermittedPrisonerDetails(
  val prisonerId: String,
  val isActive: Boolean,
)
