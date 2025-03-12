package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CREATE_BOOKER_PRISONER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.PRISONER_ADDED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository

@Transactional(propagation = SUPPORTS)
@DisplayName("Create booker prisoner $CREATE_BOOKER_PRISONER_PATH")
class CreateBookerPrisonerTest : IntegrationTestBase() {
  private val emailAddress = "aled@aled.com"
  private lateinit var booker: Booker

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @BeforeEach
  fun setup() {
    booker = createBooker(oneLoginSub = "123", emailAddress = emailAddress)
  }

  @Test
  fun `when prisoner does not exist then prisoner is created`() {
    // Given
    val createPrisoner = CreatePermittedPrisonerDto(prisonerId = "1233", active = true, prisonCode = PRISON_CODE)

    // When
    val responseSpec = callCreateBookerPrisoner(bookerConfigServiceRoleHttpHeaders, createPrisoner, booker.reference)

    // Then
    responseSpec.expectStatus().isCreated
    val dto = getPermittedPrisonerDto(responseSpec)

    assertThat(dto).isNotNull()
    assertThat(dto.prisonerId).isEqualTo(createPrisoner.prisonerId)
    assertThat(dto.active).isTrue()
    assertThat(dto.permittedVisitors).isEmpty()
    assertThat(dto.prisonCode).isEqualTo(PRISON_CODE)

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      PRISONER_ADDED.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to createPrisoner.prisonerId,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, PRISONER_ADDED, "Prisoner with prisonNumber - ${createPrisoner.prisonerId} added to booker")
  }

  @Test
  fun `when prisoner already exist an exception is thrown`() {
    // Given
    val createPrisoner = CreatePermittedPrisonerDto(prisonerId = "1233", active = true, prisonCode = PRISON_CODE)

    val prisoner = createPrisoner(booker, createPrisoner.prisonerId)
    booker.permittedPrisoners.add(prisoner)
    bookerRepository.saveAndFlush(booker)
    // When
    val responseSpec = callCreateBookerPrisoner(bookerConfigServiceRoleHttpHeaders, createPrisoner, booker.reference)

    // Then
    assertError(responseSpec, "Booker prisoner already exists", "BookerPrisoner for ${booker.reference} already exists", BAD_REQUEST)
  }

  @Test
  fun `when booker not does exist then exception is thrown`() {
    // Given
    val createPrisoner = CreatePermittedPrisonerDto(prisonerId = "1233", active = true, prisonCode = PRISON_CODE)
    val bookerReference = "IDontExist"

    // When
    val responseSpec = callCreateBookerPrisoner(bookerConfigServiceRoleHttpHeaders, createPrisoner, bookerReference)

    // Then
    assertError(responseSpec, "Booker not found", "Booker for reference : $bookerReference not found", NOT_FOUND)
  }

  @Test
  fun `when prisonerId is not given exception is thrown`() {
    // Given
    val createPrisoner = CreatePermittedPrisonerDto(prisonerId = "", active = true, prisonCode = PRISON_CODE)

    // When
    val responseSpec = callCreateBookerPrisoner(bookerConfigServiceRoleHttpHeaders, createPrisoner, booker.reference)

    // Then
    assertError(responseSpec, "Invalid Argument in request", "NotBlank.createPermittedPrisonerDto.prisonerId", BAD_REQUEST)
  }

  @Test
  fun `when prisoner end point is call with incorrect role`() {
    // Given
    val createPrisoner = CreatePermittedPrisonerDto(prisonerId = "1233", active = true, prisonCode = PRISON_CODE)

    // When
    val responseSpec = callCreateBookerPrisoner(orchestrationServiceRoleHttpHeaders, createPrisoner, "IDontExist")

    // Then
    responseSpec
      .expectStatus().isForbidden
  }
}
