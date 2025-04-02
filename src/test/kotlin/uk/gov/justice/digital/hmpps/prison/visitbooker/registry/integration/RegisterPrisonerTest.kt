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
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CREATE_BOOKER_PRISONER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.PRISONER_REGISTERED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.REGISTER_PRISONER_SEARCH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError.BOOKER_ALREADY_HAS_A_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError.DOB_INCORRECT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError.FIRST_NAME_INCORRECT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError.LAST_NAME_INCORRECT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError.PRISONER_ALREADY_EXISTS_FOR_BOOKER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError.PRISONER_NOT_FOUND
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
  lateinit var prisonerOffenderSearchClientSpy: PrisonerOffenderSearchClient

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

    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    responseSpec.expectStatus().isCreated

    verify(bookerAuditRepositorySpy, times(2)).saveAndFlush(any<BookerAudit>())
    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)

    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to firstName,
        "lastNameEntered" to lastName,
        "dobEntered" to dateOfBirth.toString(),
        "prisonCodeEntered" to prisonCode,
        "success" to true.toString(),
      ),
      null,
    )

    verify(telemetryClientSpy, times(1)).trackEvent(
      PRISONER_REGISTERED.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(2)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} was successful")
    assertAuditEvent(auditEvents[1], booker.reference, PRISONER_REGISTERED, "Prisoner with prisonNumber - ${registerPrisoner.prisonerId} registered against booker")
  }

  @Test
  fun `when register prisoner sent with different case and with spaces it still matches the prisoner is successfully registered against the booker`() {
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = "    $firstName    ".uppercase(),
      prisonerLastName = "    $lastName    ".uppercase(),
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

    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    responseSpec.expectStatus().isCreated

    verify(bookerAuditRepositorySpy, times(2)).saveAndFlush(any<BookerAudit>())
    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)

    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to registerPrisoner.prisonerFirstName,
        "lastNameEntered" to registerPrisoner.prisonerLastName,
        "dobEntered" to registerPrisoner.prisonerDateOfBirth.toString(),
        "prisonCodeEntered" to registerPrisoner.prisonCode,
        "success" to true.toString(),
      ),
      null,
    )

    verify(telemetryClientSpy, times(1)).trackEvent(
      PRISONER_REGISTERED.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(2)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} was successful")
    assertAuditEvent(auditEvents[1], booker.reference, PRISONER_REGISTERED, "Prisoner with prisonNumber - ${registerPrisoner.prisonerId} registered against booker")
  }

  @Test
  fun `when register prisoner does not match the prisoner is not registered against the booker and fails with a validation error`() {
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = prisonerId,
      prisonCode = prisonCode,
      prisonerFirstName = "different-firstname",
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

    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    assertError(
      responseSpec,
      "Prisoner registration validation failed",
      "Prisoner registration validation failed with the following errors - FIRST_NAME_INCORRECT",
      org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
    )

    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(telemetryClientSpy, times(0)).trackEvent(any(), any(), any())

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} failed with errors - FIRST_NAME_INCORRECT")
    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to registerPrisoner.prisonerFirstName,
        "lastNameEntered" to registerPrisoner.prisonerLastName,
        "dobEntered" to registerPrisoner.prisonerDateOfBirth.toString(),
        "prisonCodeEntered" to registerPrisoner.prisonCode,
        "success" to false.toString(),
        "validationErrors" to FIRST_NAME_INCORRECT.toString(),
      ),
      null,
    )
  }

  @Test
  fun `when register prisoner does not match and fails with multiple errors the prisoner is not registered against the booker and fails with a validation error`() {
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = "different-firstname",
      prisonerLastName = "different-lastname",
      prisonerDateOfBirth = dateOfBirth.plusYears(1),
    )

    val prisonerOffenderDetails = createPrisonerDto(
      prisonerNumber = prisonerId,
      prisonId = prisonCode,
      inOutStatus = null,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
    )

    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    assertError(
      responseSpec,
      "Prisoner registration validation failed",
      "Prisoner registration validation failed with the following errors - FIRST_NAME_INCORRECT, LAST_NAME_INCORRECT, DOB_INCORRECT",
      org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
    )

    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    verify(telemetryClientSpy, times(0)).trackEvent(any(), any(), any())

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} failed with errors - FIRST_NAME_INCORRECT, LAST_NAME_INCORRECT, DOB_INCORRECT")
    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to registerPrisoner.prisonerFirstName,
        "lastNameEntered" to registerPrisoner.prisonerLastName,
        "dobEntered" to registerPrisoner.prisonerDateOfBirth.toString(),
        "prisonCodeEntered" to registerPrisoner.prisonCode,
        "success" to false.toString(),
        "validationErrors" to "$FIRST_NAME_INCORRECT, $LAST_NAME_INCORRECT, $DOB_INCORRECT",
      ),
      null,
    )
  }

  @Test
  fun `when prisoner already exists the prisoner is not registered against the booker and fails with a validation error`() {
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
    assertError(
      responseSpec,
      "Prisoner registration validation failed",
      "Prisoner registration validation failed with the following errors - PRISONER_ALREADY_EXISTS_FOR_BOOKER",
      org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
    )

    verify(prisonerOffenderSearchClientSpy, times(0)).getPrisonerById(prisonerId)
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} failed with errors - PRISONER_ALREADY_EXISTS_FOR_BOOKER")
    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to registerPrisoner.prisonerFirstName,
        "lastNameEntered" to registerPrisoner.prisonerLastName,
        "dobEntered" to registerPrisoner.prisonerDateOfBirth.toString(),
        "prisonCodeEntered" to registerPrisoner.prisonCode,
        "success" to false.toString(),
        "validationErrors" to PRISONER_ALREADY_EXISTS_FOR_BOOKER.toString(),
      ),
      null,
    )
  }

  @Test
  fun `when booker already has an active prisoner the prisoner is not registered against the booker and fails with a validation error`() {
    // Given
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )

    // an active prisoner already exists for booker
    val prisoner = createPrisoner(booker, "AB123456", active = true)
    booker.permittedPrisoners.add(prisoner)
    bookerRepository.saveAndFlush(booker)
    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    assertError(
      responseSpec,
      "Prisoner registration validation failed",
      "Prisoner registration validation failed with the following errors - BOOKER_ALREADY_HAS_A_PRISONER",
      org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
    )

    verify(prisonerOffenderSearchClientSpy, times(0)).getPrisonerById(prisonerId)
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} failed with errors - BOOKER_ALREADY_HAS_A_PRISONER")
    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to registerPrisoner.prisonerFirstName,
        "lastNameEntered" to registerPrisoner.prisonerLastName,
        "dobEntered" to registerPrisoner.prisonerDateOfBirth.toString(),
        "prisonCodeEntered" to registerPrisoner.prisonCode,
        "success" to false.toString(),
        "validationErrors" to BOOKER_ALREADY_HAS_A_PRISONER.toString(),
      ),
      null,
    )
  }

  @Test
  fun `when prisoner not found on prisoner search the prisoner is not registered against the booker and fails with a validation error`() {
    // Given
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )

    // prisoner not found on prisoner-search
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, null, HttpStatus.SC_NOT_FOUND)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    assertError(
      responseSpec,
      "Prisoner registration validation failed",
      "Prisoner registration validation failed with the following errors - PRISONER_NOT_FOUND",
      org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
    )

    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, REGISTER_PRISONER_SEARCH, "Prisoner search for prisonNumber - ${registerPrisoner.prisonerId}, firstName: ${registerPrisoner.prisonerFirstName}, lastName: ${registerPrisoner.prisonerLastName}, DOB: ${registerPrisoner.prisonerDateOfBirth}, prisonCode: ${registerPrisoner.prisonCode} failed with errors - PRISONER_NOT_FOUND")
    verify(telemetryClientSpy, times(1)).trackEvent(
      REGISTER_PRISONER_SEARCH.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to registerPrisoner.prisonerId,
        "firstNameEntered" to registerPrisoner.prisonerFirstName,
        "lastNameEntered" to registerPrisoner.prisonerLastName,
        "dobEntered" to registerPrisoner.prisonerDateOfBirth.toString(),
        "prisonCodeEntered" to registerPrisoner.prisonCode,
        "success" to false.toString(),
        "validationErrors" to PRISONER_NOT_FOUND.toString(),
      ),
      null,
    )
  }

  @Test
  fun `when prisoner search throws INTERNAL_SERVER_ERROR the prisoner is not registered against the booker and fails with a validation error`() {
    // Given
    val registerPrisoner = RegisterPrisonerRequestDto(
      prisonerId = "1233",
      prisonCode = prisonCode,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
    )

    // prisoner not found on prisoner-search
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, null, HttpStatus.SC_INTERNAL_SERVER_ERROR)

    // When
    val responseSpec = callRegisterPrisoner(orchestrationServiceRoleHttpHeaders, registerPrisoner, booker.reference)

    // Then
    responseSpec.expectStatus().isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
    verify(prisonerOffenderSearchClientSpy, times(1)).getPrisonerById(prisonerId)
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)
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
    assertError(
      responseSpec,
      userMessage = "Booker not found",
      developerMessage = "Booker for reference : $bookerReference not found",
      NOT_FOUND,
    )
    verify(prisonerOffenderSearchClientSpy, times(0)).getPrisonerById(prisonerId)
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)
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
