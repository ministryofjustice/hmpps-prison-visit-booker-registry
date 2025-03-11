package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.DEACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerAuditService

@DisplayName("Deactivate booker prisoner")
class DeactivatePrisonerByBookerReferenceTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var bookerAuditServiceSpy: BookerAuditService

  private lateinit var booker: Booker

  private lateinit var prisoner1: PermittedPrisonerTestObject
  private lateinit var prisoner2: PermittedPrisonerTestObject

  @Autowired
  private lateinit var prisonerRepository: PermittedPrisonerRepository

  @BeforeEach
  internal fun setUp() {
    booker = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    prisoner1 = PermittedPrisonerTestObject("AB123451", PRISON_CODE, true)
    prisoner2 = PermittedPrisonerTestObject("AB123452", PRISON_CODE, true)

    createAssociatedPrisoners(
      booker,
      listOf(prisoner1, prisoner2),
    )
  }

  @Test
  fun `when prisoner is deactivated for booker all data is persisted and returned correctly`() {
    // Given

    // When
    val responseSpec = deactivatePrisonersByBookerReference(webTestClient, booker.reference, prisoner1.prisonerId, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
    val associatedPrisoner = getResults(returnResult.expectBody())

    Assertions.assertThat(associatedPrisoner.active).isFalse()

    val permittedPrisoners = prisonerRepository.findByBookerId(booker.id)
    Assertions.assertThat(permittedPrisoners.first { prisoner1.prisonerId == it.prisonerId }.active).isFalse
    Assertions.assertThat(permittedPrisoners.first { prisoner2.prisonerId == it.prisonerId }.active).isTrue
    verify(bookerAuditServiceSpy, times(1)).auditBookerEvent(booker.reference, "Prisoner with prisonNumber - ${prisoner1.prisonerId} deactivated")
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = deactivatePrisonersByBookerReference(webTestClient, "invalid-reference", prisonerId = "IDontExist", bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    // When
    val responseSpec = deactivatePrisonersByBookerReference(webTestClient, booker.reference, prisonerId = "IDontExist", setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PermittedPrisonerDto = objectMapper.readValue(returnResult.returnResult().responseBody, PermittedPrisonerDto::class.java)

  fun deactivatePrisonersByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = DEACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
