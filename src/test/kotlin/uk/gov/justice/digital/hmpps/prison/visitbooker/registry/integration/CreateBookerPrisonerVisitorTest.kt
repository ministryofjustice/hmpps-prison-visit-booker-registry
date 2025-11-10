package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.CREATE_BOOKER_PRISONER_VISITOR_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.VISITOR_ADDED_TO_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.SnsService

@Transactional(propagation = SUPPORTS)
@DisplayName("Create booker prisoner visitor $CREATE_BOOKER_PRISONER_VISITOR_PATH")
class CreateBookerPrisonerVisitorTest : IntegrationTestBase() {

  private val emailAddress = "aled@aled.com"
  private lateinit var booker: Booker
  private lateinit var prisoner: PermittedPrisoner

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @MockitoSpyBean
  lateinit var snsService: SnsService

  @BeforeEach
  fun setup() {
    booker = createBooker(oneLoginSub = "123", emailAddress = emailAddress)
    prisoner = createPrisoner(booker, prisonerId = "AZ1234")
    booker.permittedPrisoners.add(prisoner)
    bookerRepository.saveAndFlush(booker)
  }

  @Test
  fun `when visitor does not exist then visitor is created and email is sent`() {
    // Given
    val createVisitorDto = CreatePermittedVisitorDto(visitorId = 1233, active = true, sendNotificationFlag = true)

    // When
    val responseSpec = callCreateBookerPrisonerVisitor(bookerConfigServiceRoleHttpHeaders, createVisitorDto, bookerReference = booker.reference, prisonerId = prisoner.prisonerId)

    // Then
    responseSpec.expectStatus().isCreated
    val dto = getPermittedVisitorDto(responseSpec)

    assertThat(dto).isNotNull()
    assertThat(dto.visitorId).isEqualTo(createVisitorDto.visitorId)
    assertThat(dto.active).isTrue()

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      eq(VISITOR_ADDED_TO_PRISONER.telemetryEventName),
      org.mockito.kotlin.check {
        assertThat(it["bookerReference"]).isEqualTo(booker.reference)
        assertThat(it["prisonerId"]).isEqualTo(prisoner.prisonerId)
        assertThat(it["visitorId"]).isEqualTo(createVisitorDto.visitorId.toString())
      },
      isNull(),
    )
    verify(snsService, times(1)).sendBookerPrisonerVisitorApprovedEvent(booker.reference, prisoner.prisonerId, createVisitorDto.visitorId.toString())

    verify(telemetryClientSpy, times(1)).trackEvent(
      eq("prison-visit-booker.visitor-approved-domain-event"),
      org.mockito.kotlin.check {
        assertThat(it["bookerReference"]).isEqualTo(booker.reference)
        assertThat(it["prisonerId"]).isEqualTo(prisoner.prisonerId)
        assertThat(it["visitorId"]).isEqualTo(createVisitorDto.visitorId.toString())
      },
      isNull(),
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, VISITOR_ADDED_TO_PRISONER, "Visitor ID - ${createVisitorDto.visitorId} added to prisoner - ${prisoner.prisonerId}")
  }

  @Test
  fun `when visitor does not exist then visitor is created but email flag is false, no email is sent`() {
    // Given
    val createVisitorDto = CreatePermittedVisitorDto(visitorId = 1233, active = true, sendNotificationFlag = false)

    // When
    val responseSpec = callCreateBookerPrisonerVisitor(bookerConfigServiceRoleHttpHeaders, createVisitorDto, bookerReference = booker.reference, prisonerId = prisoner.prisonerId)

    // Then
    responseSpec.expectStatus().isCreated
    val dto = getPermittedVisitorDto(responseSpec)

    assertThat(dto).isNotNull()
    assertThat(dto.visitorId).isEqualTo(createVisitorDto.visitorId)
    assertThat(dto.active).isTrue()

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      eq(VISITOR_ADDED_TO_PRISONER.telemetryEventName),
      org.mockito.kotlin.check {
        assertThat(it["bookerReference"]).isEqualTo(booker.reference)
        assertThat(it["prisonerId"]).isEqualTo(prisoner.prisonerId)
        assertThat(it["visitorId"]).isEqualTo(createVisitorDto.visitorId.toString())
      },
      isNull(),
    )
    verifyNoInteractions(snsService)

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, VISITOR_ADDED_TO_PRISONER, "Visitor ID - ${createVisitorDto.visitorId} added to prisoner - ${prisoner.prisonerId}")
  }

  @Test
  fun `when visitor already exist, they're not re-added and 200 is returned`() {
    // Given
    val createVisitor = CreatePermittedVisitorDto(visitorId = 1233, active = true)

    val visitor = createVisitor(permittedPrisoner = prisoner, createVisitor.visitorId)
    prisoner.permittedVisitors.add(visitor)
    permittedPrisonerRepository.saveAndFlush(prisoner)
    // When
    val responseSpec = callCreateBookerPrisonerVisitor(bookerConfigServiceRoleHttpHeaders, createVisitor, booker.reference, prisonerId = prisoner.prisonerId)

    // Then
    responseSpec.expectStatus().is2xxSuccessful
  }

  @Test
  fun `when booker reference not does exist then exception is thrown`() {
    // Given
    val createVisitor = CreatePermittedVisitorDto(visitorId = 1233, active = true)
    val bookerReference = "IDontExist"

    // When
    val responseSpec = callCreateBookerPrisonerVisitor(bookerConfigServiceRoleHttpHeaders, createVisitor, bookerReference, prisonerId = prisoner.prisonerId)

    // Then
    assertError(responseSpec, "Permitted prisoner not found for booker", "Permitted prisoner for - IDontExist/AZ1234 not found", NOT_FOUND)
  }

  @Test
  fun `when prisonerId not does exist then exception is thrown`() {
    // Given
    val createVisitor = CreatePermittedVisitorDto(visitorId = 1233, active = true)
    val bookerReference = booker.reference

    // When
    val responseSpec = callCreateBookerPrisonerVisitor(bookerConfigServiceRoleHttpHeaders, createVisitor, bookerReference, prisonerId = "IDontExist")

    // Then
    assertError(responseSpec, "Permitted prisoner not found for booker", "Permitted prisoner for - $bookerReference/IDontExist not found", NOT_FOUND)
  }

  @Test
  fun `when end point is call with incorrect role`() {
    // Given
    val createVisitor = CreatePermittedVisitorDto(visitorId = 1233, active = true)
    val bookerReference = booker.reference

    // When
    val responseSpec = callCreateBookerPrisonerVisitor(orchestrationServiceRoleHttpHeaders, createVisitor, bookerReference, prisonerId = prisoner.prisonerId)

    // Then
    responseSpec
      .expectStatus().isForbidden
  }
}
