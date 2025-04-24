package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.CREATE_BOOKER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.BOOKER_CREATED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository

@Transactional(propagation = SUPPORTS)
@DisplayName("Create booker $CREATE_BOOKER_PATH")
class CreateBookerTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @Test
  fun `when booker does not exist then booker is created with all child objects`() {
    // Given
    val emailAddress = "aled@aled.com"
    val createBookerDto = CreateBookerDto(email = emailAddress)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    responseSpec.expectStatus().isCreated

    val createdEntity = bookerRepository.findByEmailIgnoreCase(emailAddress)
    assertThat(createdEntity).isNotNull

    val dto = getBookerDto(responseSpec)
    assertThat(dto.reference).hasSizeGreaterThan(9)
    assertThat(dto.reference).isEqualTo(createdEntity!!.reference)
    assertThat(dto.oneLoginSub).isNull()
    assertThat(dto.email).isEqualTo(emailAddress)
    assertThat(dto.permittedPrisoners).isEmpty()

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      BOOKER_CREATED.telemetryEventName,
      mapOf(
        "bookerReference" to dto.reference,
        "email" to emailAddress,
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], dto.reference, BOOKER_CREATED, "Booker created (without sub) with email - $emailAddress")
  }

  @Test
  fun `when booker does exist then exception is thrown`() {
    // Given
    val emailAddress = "aled@aled.com"

    val oneLoginSub = "123"
    val booker = createBooker(oneLoginSub = oneLoginSub, emailAddress = emailAddress)
    bookerRepository.saveAndFlush(booker)

    val createBookerDto = CreateBookerDto(email = emailAddress)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    assertError(responseSpec, "Booker already exists", "The given email - $emailAddress already exists", BAD_REQUEST)
  }

  @Test
  fun `when booker email is not given exception is thrown`() {
    // Given
    val createBookerDto = CreateBookerDto(email = "")

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    assertError(responseSpec, "Invalid Argument: email", "must not be blank", BAD_REQUEST)
  }

  @Test
  fun `when booker end point is call with incorrect role`() {
    // Given
    val createBookerDto = CreateBookerDto(email = "aled@aled.com")

    // When
    val responseSpec = callCreateBooker(orchestrationServiceRoleHttpHeaders, createBookerDto)

    // Then
    responseSpec
      .expectStatus().isForbidden
  }

  @Test
  fun `when booker details are cleared all child objects are removed`() {
    // Given
    val emailAddress = "aled@aled.com"
    val oneLoginSub = "123"
    val booker = createBooker(oneLoginSub = oneLoginSub, emailAddress = emailAddress)
    val prisoner = createPrisoner(booker, prisonerId = "IM GONE")
    booker.permittedPrisoners.add(prisoner)
    val visitor = createVisitor(prisoner, visitorId = 0L)
    prisoner.permittedVisitors.add(visitor)
    bookerRepository.saveAndFlush(booker)

    // When
    val responseSpec = callClearBookerDetails(bookerConfigServiceRoleHttpHeaders, booker.reference)

    // Then
    responseSpec.expectStatus().isOk
    val dto = getBookerDto(responseSpec)
    assertThat(dto.email).isEqualTo(booker.email)
    assertThat(dto.reference).isEqualTo(booker.reference)
    assertThat(dto.oneLoginSub).isEqualTo(booker.oneLoginSub)
    assertThat(dto.permittedPrisoners).isEmpty()

    val savedBooker = bookerRepository.findByReference(booker.reference)
    assertThat(savedBooker).isNotNull
    savedBooker?.let {
      assertThat(savedBooker.email).isEqualTo(booker.email)
      assertThat(savedBooker.reference).isEqualTo(booker.reference)
      assertThat(savedBooker.oneLoginSub).isEqualTo(booker.oneLoginSub)
      assertThat(savedBooker.permittedPrisoners).isEmpty()
    }
  }
}
