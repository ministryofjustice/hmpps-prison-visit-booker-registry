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
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.UPDATE_PERMITTED_PRISONER_PRISON
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.UpdateRegisteredPrisonerPrisonDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@DisplayName("Update a permitted prisoner's registered prison")
class UpdatePermittedPrisonerPrisonByBookerReferenceTest : IntegrationTestBase() {
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
    prisoner1 = PermittedPrisonerTestObject("AB123451", PRISON_CODE)
    prisoner2 = PermittedPrisonerTestObject("AB123452", PRISON_CODE)

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
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto(newPrisonCode)
    val responseSpec = updatePermittedPrisonerPrisonByBookerReference(webTestClient, booker.reference, prisonerId, updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
    val associatedPrisoner = getResults(returnResult.expectBody())
    assertThat(associatedPrisoner.prisonerId).isEqualTo(prisonerId)
    assertThat(associatedPrisoner.prisonCode).isEqualTo(newPrisonCode)

    val permittedPrisoners = prisonerRepository.findByBookerId(booker.id)
    assertThat(permittedPrisoners.first { it.prisonerId == prisonerId }.prisonCode).isEqualTo(newPrisonCode)

    verify(telemetryClientSpy, times(1)).trackEvent(
      BookerAuditType.UPDATE_REGISTERED_PRISONER_PRISON.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to prisoner1.prisonerId,
        "newPrisonCode" to newPrisonCode,
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, BookerAuditType.UPDATE_REGISTERED_PRISONER_PRISON, text = "Prisoner with prisonNumber - $prisonerId had prison code updated to $newPrisonCode for booker reference - ${booker.reference}")
  }

  @Test
  fun `when prisoner's prison is updated for a prisoner but prisoner is not in new prison then prison code for the prisoner is not updated`() {
    // Given
    val newPrisonCode = "MDI"

    val prisonerId = prisoner1.prisonerId
    val oldPrisonCode = prisoner1.prisonCode

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
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto(newPrisonCode)
    val responseSpec = updatePermittedPrisonerPrisonByBookerReference(webTestClient, booker.reference, prisonerId, updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Updating a prisoner's prison failed")
      .jsonPath("$.developerMessage")
      .isEqualTo("Updating a prisoner's prison failed with the following error - Prisoner - $prisonerId is in $oldPrisonCode - so cannot be updated to $newPrisonCode")
  }

  @Test
  fun `when prisoner's prison is updated for a prisoner but prisoner has not been added for a booker the prison code is not updated`() {
    // Given
    val prisonCode = "MDI"
    val prisonerId = "non-existent-prisoner"

    val registeredPrisoner = PrisonerDto(
      prisonerNumber = prisonerId,
      prisonId = prisonCode,
      inOutStatus = null,
      firstName = "test",
      lastName = "user",
      dateOfBirth = null,
    )

    // When
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, registeredPrisoner)
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto(prisonCode)
    val responseSpec = updatePermittedPrisonerPrisonByBookerReference(webTestClient, booker.reference, prisonerId, updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Updating a prisoner's prison failed")
      .jsonPath("$.developerMessage")
      .isEqualTo("Updating a prisoner's prison failed with the following error - Prisoner $prisonerId has not been added for booker ${booker.reference}")
  }

  @Test
  fun `when booker is not found a 404 status error is returned`() {
    // Given
    val newPrisonCode = "MDI"
    val nonExistentBookerReference = "non-existent-booker-reference"

    val prisonerId = prisoner1.prisonerId

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
    val updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto(newPrisonCode)
    val responseSpec = updatePermittedPrisonerPrisonByBookerReference(webTestClient, bookerReference = nonExistentBookerReference, prisonerId, updateRegisteredPrisonerPrisonDto = updateRegisteredPrisonerPrisonDto, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Booker not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Booker for reference : non-existent-booker-reference not found")
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    // When
    val responseSpec = updatePermittedPrisonerPrisonByBookerReference(webTestClient, booker.reference, prisonerId = prisoner1.prisonerId, updateRegisteredPrisonerPrisonDto = UpdateRegisteredPrisonerPrisonDto("IDontExist"), setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PermittedPrisonerDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PermittedPrisonerDto::class.java)

  fun updatePermittedPrisonerPrisonByBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    updateRegisteredPrisonerPrisonDto: UpdateRegisteredPrisonerPrisonDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = UPDATE_PERMITTED_PRISONER_PRISON.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(updateRegisteredPrisonerPrisonDto))
      .exchange()
  }
}
