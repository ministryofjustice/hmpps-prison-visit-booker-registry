package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.UPDATE_BOOKER_PRISONER_PRISON_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@DisplayName("Deactivate booker prisoner")
class UpdatePrisonerPrisonByBookerReferenceTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

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
  fun `when prisoner's prison is updated for a prisoner and validation is successful then prison code for the prisoner is updated successfully`() {
    // Given
    val newPrisonCode = "MDI"
    val prisonerId = prisoner1.prisonerId
    val oldPrisonCode = prisoner1.prisonCode
    val registeredPrisoner = PrisonerDto(
      prisonerNumber = prisoner1.prisonerId,
      prisonId = newPrisonCode,
      inOutStatus = null,
      firstName = "test",
      lastName = "user",
      dateOfBirth = null,
    )

    // When
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, registeredPrisoner)
    val responseSpec = updatePrisonersPrisonByBookerReference(webTestClient, booker.reference, prisonerId, newPrisonCode = newPrisonCode, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
    val associatedPrisoner = getResults(returnResult.expectBody())
    assertThat(associatedPrisoner.prisonerId).isEqualTo(prisonerId)
    assertThat(associatedPrisoner.prisonCode).isEqualTo(newPrisonCode)

    val permittedPrisoners = prisonerRepository.findByBookerId(booker.id)
    assertThat(permittedPrisoners.first { it.prisonerId == prisonerId }.prisonCode).isEqualTo(newPrisonCode)

    verify(telemetryClientSpy, times(1)).trackEvent(
      BookerAuditType.UPDATE_PRISONER_PRISON.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to prisoner1.prisonerId,
        "oldPrisonCode" to oldPrisonCode,
        "newPrisonCode" to newPrisonCode,
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, BookerAuditType.UPDATE_PRISONER_PRISON, text = "Prisoner with prisonNumber - $prisonerId had prison code updated from ${prisoner1.prisonCode} to $newPrisonCode for booker reference - ${booker.reference}")
  }

  @Test
  fun `when prisoner's prison is updated for a prisoner but prisoner is not in new prison then prison code for the prisoner is not updated`() {
    // Given
    val newPrisonCode = "MDI"

    val prisonerId = prisoner1.prisonerId
    val oldPrisonCode = prisoner1.prisonCode

    // prisoner is not in MDI but still in HEI
    val registeredPrisoner = PrisonerDto(
      prisonerNumber = prisoner1.prisonerId,
      prisonId = oldPrisonCode,
      inOutStatus = null,
      firstName = "test",
      lastName = "user",
      dateOfBirth = null,
    )

    // When
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, registeredPrisoner)
    val responseSpec = updatePrisonersPrisonByBookerReference(webTestClient, booker.reference, prisonerId, newPrisonCode = newPrisonCode, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Updating a prisoner's prison failed")
      .jsonPath("$.developerMessage")
      .isEqualTo("Updating a prisoner's prison failed with the following error - Prisoner - $prisonerId is in HEI - so cannot be updated to $newPrisonCode")
  }
/*
  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = updatePrisonersPrisonByBookerReference(webTestClient, "invalid-reference", prisonerId = "IDontExist", bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    // When
    val responseSpec = updatePrisonersPrisonByBookerReference(webTestClient, booker.reference, prisonerId = "IDontExist", setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }*/

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PermittedPrisonerDto = objectMapper.readValue(returnResult.returnResult().responseBody, PermittedPrisonerDto::class.java)

  fun updatePrisonersPrisonByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    newPrisonCode: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = UPDATE_BOOKER_PRISONER_PRISON_CONTROLLER_PATH.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId).replace("{prisonId}", newPrisonCode)
    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
