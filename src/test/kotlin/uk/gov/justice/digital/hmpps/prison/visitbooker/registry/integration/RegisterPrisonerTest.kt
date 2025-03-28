package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CREATE_BOOKER_PRISONER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.PRISONER_ADDED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@DisplayName("Create booker prisoner $CREATE_BOOKER_PRISONER_PATH")
class RegisterPrisonerTest : IntegrationTestBase() {
  private val emailAddress = "test@example.com"
  private lateinit var booker: Booker

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  // Given
  private val prisonerId = "1233"
  private val prisonCode = PRISON_CODE
  private val firstName = "Test"
  private val lastName = "User"
  private val dateOfBirth = LocalDate.of(1901, 1, 1)

  @BeforeEach
  fun setup() {
    booker = createBooker(oneLoginSub = "123", emailAddress = emailAddress)
  }

  @Test
  fun `when register prisoner matches the prisoner is successfully registered against the booker`() {
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )

    val prisonerOffenderDetails = createPrisonerDto(
      prisonerNumber = prisonerId,
      prisonId = prisonCode,
      inOutStatus = null,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
    )

    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails, HttpStatus.SC_NOT_FOUND)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    responseSpec.expectStatus().isCreated
    val dto = getPermittedPrisonerDto(responseSpec)

    assertThat(dto).isNotNull()
    assertThat(dto.prisonerId).isEqualTo(registerPrisoner.prisonerId)
    assertThat(dto.active).isTrue()
    assertThat(dto.permittedVisitors).isEmpty()
    assertThat(dto.prisonCode).isEqualTo(PRISON_CODE)

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      PRISONER_ADDED.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, PRISONER_ADDED, "Prisoner with prisonNumber - ${registerPrisoner.prisonerId} added to booker")
  }

  @Test
  fun `when prisoner already exists an exception is thrown`() {
    // Given
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )

    // prisoner already exists for booker
    val prisoner = createPrisoner(booker, registerPrisoner.prisonerId)
    booker.permittedPrisoners.add(prisoner)
    bookerRepository.saveAndFlush(booker)
    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)

    // TODO
    // assertError(responseSpec, "Booker prisoner already exists", "BookerPrisoner for ${booker.reference} already exists", BAD_REQUEST)
  }

  @Test
  fun `when booker does not exist then exception is thrown`() {
    // Given
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )
    val bookerReference = "IDontExist"

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, bookerReference)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.SC_NOT_FOUND)
    assertError(responseSpec, "Booker not found", "Booker for reference : $bookerReference not found", NOT_FOUND)
  }

  @Test
  fun `when prisoner end point is call with incorrect role`() {
    // Given
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )
    // When
    val responseSpec = callRegisterPrisoner(bookerConfigServiceRoleHttpHeaders, registerPrisoner, "IDontExist")

    // Then
    responseSpec
      .expectStatus().isForbidden
  }
}
