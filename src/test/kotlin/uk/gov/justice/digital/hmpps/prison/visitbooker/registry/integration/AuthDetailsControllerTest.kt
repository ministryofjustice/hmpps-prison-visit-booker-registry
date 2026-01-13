package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.AUTH_DETAILS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerReference
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.BOOKER_CREATED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.UPDATE_BOOKER_EMAIL
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@Transactional(propagation = SUPPORTS)
@DisplayName("Migrate $AUTH_DETAILS_CONTROLLER_PATH")
class AuthDetailsControllerTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var bookerRepositorySpy: BookerRepository

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  private val emailAddress = "test@example.com"
  private val oneLoginSub = "one-login-sub"
  private val phoneNumber = "0123456789"

  @Test
  fun `when auth details are submitted for the first time a booker entry is created`() {
    // Given
    val authDetailsDto = AuthDetailDto(oneLoginSub, emailAddress, phoneNumber)
    // When
    val responseSpec = callBookerAuth(orchestrationServiceRoleHttpHeaders, authDetailsDto)

    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).hasSizeGreaterThan(9)

    val updatedPilotBooker = bookerRepository.findAll().firstOrNull()
    assertThat(updatedPilotBooker).isNotNull
    assertThat(updatedPilotBooker!!.oneLoginSub).isEqualTo(authDetailsDto.oneLoginSub)

    verify(bookerRepositorySpy, times(1)).findByEmailIgnoreCaseAndOneLoginSub(authDetailsDto.email, authDetailsDto.oneLoginSub)
    verify(bookerRepositorySpy, times(1)).findAllByEmailIgnoreCase(authDetailsDto.email)
    verify(bookerRepositorySpy, times(1)).findByOneLoginSub(authDetailsDto.oneLoginSub)
    verify(bookerRepositorySpy, times(1)).saveAndFlush(any())
    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      BOOKER_CREATED.telemetryEventName,
      mapOf(
        "bookerReference" to updatedPilotBooker.reference,
        "email" to emailAddress,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], updatedPilotBooker.reference, BOOKER_CREATED, "Booker created with email - $emailAddress")
  }

  @Test
  fun `when auth details already exists then a reference is returned`() {
    // Given
    // already existing booker
    val booker = bookerRepository.saveAndFlush(Booker(email = emailAddress, oneLoginSub = oneLoginSub))

    // already existing booker
    val authDetailsDto = AuthDetailDto(oneLoginSub, emailAddress, phoneNumber)

    // When
    val responseSpec = callBookerAuth(orchestrationServiceRoleHttpHeaders, authDetailsDto)

    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).isEqualTo(booker.reference)
    verify(bookerRepositorySpy, times(1)).findByEmailIgnoreCaseAndOneLoginSub(authDetailsDto.email, authDetailsDto.oneLoginSub)
    verify(bookerRepositorySpy, times(0)).findAllByEmailIgnoreCase(authDetailsDto.email)
    verify(bookerRepositorySpy, times(0)).findByOneLoginSub(authDetailsDto.oneLoginSub)

    verify(bookerAuditRepositorySpy, times(0)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), any(), any())
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)
  }

  @Test
  fun `when auth details already exists but the email address is in a different case even then a reference is returned`() {
    // Given
    // booker details email address is all upper case
    val booker = bookerRepository.saveAndFlush(Booker(email = emailAddress.uppercase(), oneLoginSub = oneLoginSub))

    // authDetailsDto has email address in lower case
    val authDetailsDto = AuthDetailDto(oneLoginSub, emailAddress.lowercase(), phoneNumber)

    // When
    val responseSpec = callBookerAuth(orchestrationServiceRoleHttpHeaders, authDetailsDto)

    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).isEqualTo(booker.reference)
    verify(bookerRepositorySpy, times(1)).findByEmailIgnoreCaseAndOneLoginSub(authDetailsDto.email, authDetailsDto.oneLoginSub)
    verify(bookerRepositorySpy, times(0)).findAllByEmailIgnoreCase(authDetailsDto.email)
    verify(bookerRepositorySpy, times(0)).findByOneLoginSub(authDetailsDto.oneLoginSub)
    verify(bookerAuditRepositorySpy, times(0)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(0)).trackEvent(any(), any(), any())
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)
  }

  @Test
  fun `when booker exists with same email address but different sub re-registering will create a new entry with same email address and new sub`() {
    // Given
    val oldSub = "old-sub"
    val newSub = "new-sub"

    // booker exists with old Sub and same email address
    bookerRepository.saveAndFlush(
      Booker(email = emailAddress, oneLoginSub = oldSub),
    )

    // auth is called again with a new sub and existing email address
    val authDetailsDto = AuthDetailDto(oneLoginSub = newSub, email = emailAddress, phoneNumber = phoneNumber)

    // When
    val responseSpec = callBookerAuth(orchestrationServiceRoleHttpHeaders, authDetailsDto)

    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).hasSizeGreaterThan(9)

    val newBookerDetails = bookerRepository.findByEmailIgnoreCaseAndOneLoginSub(authDetailsDto.email, newSub)
    assertThat(newBookerDetails).isNotNull
    assertThat(reference).isEqualTo(newBookerDetails!!.reference)

    val oldBookerDetails = bookerRepository.findByEmailIgnoreCaseAndOneLoginSub(authDetailsDto.email, oldSub)
    assertThat(reference).isNotEqualTo(oldBookerDetails!!.reference)
    verify(bookerRepositorySpy, times(1)).findAllByEmailIgnoreCase(authDetailsDto.email)
    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      BOOKER_CREATED.telemetryEventName,
      mapOf(
        "bookerReference" to reference,
        "email" to authDetailsDto.email,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], reference, BOOKER_CREATED, "Booker created with email - ${authDetailsDto.email}")
  }

  @Test
  fun `when booker exists with same sub but different email address the email address is updated with the new sub`() {
    // Given
    val oldEmailAddress = emailAddress
    val newEmailAddress = "test1@example.com"

    // booker exists with Sub and old email address
    bookerRepository.saveAndFlush(
      Booker(email = oldEmailAddress, oneLoginSub = oneLoginSub),
    )

    // auth is called again with the same sub but new email address
    val authDetailsDto = AuthDetailDto(oneLoginSub = oneLoginSub, email = newEmailAddress)

    // When
    val responseSpec = callBookerAuth(orchestrationServiceRoleHttpHeaders, authDetailsDto)

    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).hasSizeGreaterThan(9)

    val oldBookerDetails = bookerRepository.findByEmailIgnoreCaseAndOneLoginSub(oldEmailAddress, oneLoginSub)
    assertThat(oldBookerDetails).isNull()

    val newBookerDetails = bookerRepository.findByEmailIgnoreCaseAndOneLoginSub(newEmailAddress, oneLoginSub)
    assertThat(newBookerDetails).isNotNull
    assertThat(reference).isEqualTo(newBookerDetails!!.reference)
    verify(bookerRepositorySpy, times(1)).updateBookerEmailAddress(reference, authDetailsDto.email)
    verify(telemetryClientSpy, times(1)).trackEvent(
      UPDATE_BOOKER_EMAIL.telemetryEventName,
      mapOf(
        "bookerReference" to reference,
        "old_email" to emailAddress,
        "new_email" to authDetailsDto.email,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], reference, UPDATE_BOOKER_EMAIL, "Booker email updated from $oldEmailAddress to $newEmailAddress for booker reference - $reference")
  }

  @Test
  fun `when auth details are submitted with in correct role an is exception thrown`() {
    // Given
    val authDetailsDto = AuthDetailDto(oneLoginSub, emailAddress, phoneNumber)

    val orchestrationServiceRoleHttpHeaders = setAuthorisation(roles = listOf("ROLE_I_AM_A_HACKER"))

    // When
    val responseSpec = callBookerAuth(orchestrationServiceRoleHttpHeaders, authDetailsDto)

    // Then
    responseSpec
      .expectStatus().isForbidden
      .expectBody()
      .jsonPath("$.userMessage").value<String> { actual ->
        assertThat(actual).startsWith("Access is forbidden")
      }
      .jsonPath("$.developerMessage").value<String> { actual ->
        assertThat(actual).startsWith("Access Denied")
      }
  }

  protected fun callBookerAuth(
    authHttpHeaders: (HttpHeaders) -> Unit,
    authDetailDto: AuthDetailDto,
  ): ResponseSpec = webTestClient.put().uri(AUTH_DETAILS_CONTROLLER_PATH)
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(authDetailDto))
    .exchange()

  protected fun getReference(responseSpec: ResponseSpec): String {
    val bookerReferenceObject = TestObjectMapper.mapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerReference::class.java)
    return bookerReferenceObject.value
  }
}
