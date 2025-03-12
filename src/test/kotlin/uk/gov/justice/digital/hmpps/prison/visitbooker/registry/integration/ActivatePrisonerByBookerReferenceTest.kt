package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.ACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.ACTIVATED_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@DisplayName("Activate booker prisoner")
class ActivatePrisonerByBookerReferenceTest : IntegrationTestBase() {

  private lateinit var booker: Booker

  private lateinit var prisoner1: PermittedPrisonerTestObject
  private lateinit var prisoner2: PermittedPrisonerTestObject

  @Autowired
  private lateinit var prisonerRepository: PermittedPrisonerRepository

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @BeforeEach
  internal fun setUp() {
    booker = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    prisoner1 = PermittedPrisonerTestObject("AB123451", PRISON_CODE, false)
    prisoner2 = PermittedPrisonerTestObject("AB123452", PRISON_CODE, false)

    createAssociatedPrisoners(
      booker,
      listOf(prisoner1, prisoner2),
    )
  }

  @Test
  fun `when prisoner is activated for booker all data is persisted and returned correctly`() {
    // Given

    // When
    val responseSpec = activatePrisonersByBookerReference(webTestClient, booker.reference, prisoner1.prisonerId, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
    val associatedPrisoner = getResults(returnResult.expectBody())

    assertThat(associatedPrisoner.active).isTrue()

    val permittedPrisoners = prisonerRepository.findByBookerId(booker.id)
    assertThat(permittedPrisoners.first { prisoner1.prisonerId == it.prisonerId }.active).isTrue
    assertThat(permittedPrisoners.first { prisoner2.prisonerId == it.prisonerId }.active).isFalse

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      ACTIVATED_PRISONER.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to associatedPrisoner.prisonerId,
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, ACTIVATED_PRISONER, "Prisoner with prisonNumber - ${associatedPrisoner.prisonerId} activated")
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // Given
    // When

    val responseSpec = activatePrisonersByBookerReference(webTestClient, "invalid-reference", prisonerId = prisoner1.prisonerId, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Permitted prisoner not found for booker")
      .jsonPath("$.developerMessage")
      .isEqualTo("Permitted prisoner for - invalid-reference/${prisoner1.prisonerId} not found")
  }

  @Test
  fun `when invalid prisoner id then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = activatePrisonersByBookerReference(webTestClient, booker.reference, prisonerId = "invalid-prisoner-id", bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Permitted prisoner not found for booker")
      .jsonPath("$.developerMessage")
      .isEqualTo("Permitted prisoner for - ${booker.reference}/invalid-prisoner-id not found")
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    // When
    val responseSpec = activatePrisonersByBookerReference(webTestClient, booker.reference, prisonerId = "IDontExist", setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PermittedPrisonerDto = objectMapper.readValue(returnResult.returnResult().responseBody, PermittedPrisonerDto::class.java)

  fun activatePrisonersByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = ACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
