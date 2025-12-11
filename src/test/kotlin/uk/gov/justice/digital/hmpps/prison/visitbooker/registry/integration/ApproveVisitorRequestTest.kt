package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.APPROVE_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.LinkVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.VISITOR_REQUEST_APPROVED_FOR_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.APPROVED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerDetailsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerDetailsStoreService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.SnsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsService
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@DisplayName("Approve visitor request - $APPROVE_VISITOR_REQUEST")
class ApproveVisitorRequestTest : IntegrationTestBase() {

  @Autowired
  private lateinit var visitorRequestsRepository: VisitorRequestsRepository

  @MockitoSpyBean
  private lateinit var visitorRequestsServiceSpy: VisitorRequestsService

  @MockitoSpyBean
  private lateinit var bookerDetailsServiceSpy: BookerDetailsService

  @MockitoSpyBean
  private lateinit var bookerDetailsStoreServiceSpy: BookerDetailsStoreService

  @MockitoSpyBean
  private lateinit var visitorRequestsRepositorySpy: VisitorRequestsRepository

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @MockitoSpyBean
  lateinit var snsService: SnsService

  @Test
  fun `when a visitor request is approved the visitor is linked to the booker and a CREATED response is returned`() {
    // Given
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val bookerReference = booker.reference
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)
    val prisonerId = prisoner.prisonerId
    val visitorIdToBeLinked = 12345L
    val request = createVisitorRequest(bookerReference, prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REQUESTED)
    val requestReference = request.reference

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, requestReference, visitorIdToBeLinked, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isCreated.expectBody()
    val permittedPrisoner = bookerRepository.findByReference(bookerReference)?.permittedPrisoners?.first { it.prisonerId == prisonerId }
    Assertions.assertThat(permittedPrisoner!!.permittedVisitors.size).isEqualTo(1)
    Assertions.assertThat(permittedPrisoner.permittedVisitors[0].visitorId).isEqualTo(visitorIdToBeLinked)

    val visitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference)
    Assertions.assertThat(visitorRequest!!.status).isEqualTo(APPROVED)

    verify(visitorRequestsServiceSpy, times(1)).approveAndLinkVisitorRequest(requestReference, LinkVisitorRequestDto(visitorIdToBeLinked))
    verify(bookerDetailsServiceSpy, times(1)).createBookerPrisonerVisitor(bookerReference, prisonerId, LinkVisitorRequestDto(visitorIdToBeLinked), request.reference)
    verify(bookerDetailsStoreServiceSpy, times(1)).storeBookerPrisonerVisitor(bookerReference, prisonerId, visitorId = visitorIdToBeLinked)
    verify(visitorRequestsRepositorySpy, times(1)).approveVisitorRequest(any(), any())

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      eq(VISITOR_REQUEST_APPROVED_FOR_PRISONER.telemetryEventName),
      org.mockito.kotlin.check {
        assertThat(it["bookerReference"]).isEqualTo(bookerReference)
        assertThat(it["prisonerId"]).isEqualTo(prisonerId)
        assertThat(it["visitorId"]).isEqualTo(visitorIdToBeLinked.toString())
        assertThat(it["requestReference"]).isEqualTo(requestReference)
      },
      isNull(),
    )
    verify(snsService, times(1)).sendBookerPrisonerVisitorApprovedEvent(bookerReference, prisonerId, visitorIdToBeLinked.toString())

    verify(telemetryClientSpy, times(1)).trackEvent(
      eq("prison-visit-booker.visitor-approved-domain-event"),
      org.mockito.kotlin.check {
        assertThat(it["bookerReference"]).isEqualTo(bookerReference)
        assertThat(it["prisonerId"]).isEqualTo(prisonerId)
        assertThat(it["visitorId"]).isEqualTo(visitorIdToBeLinked.toString())
      },
      isNull(),
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], bookerReference, VISITOR_REQUEST_APPROVED_FOR_PRISONER, "Visitor ID - $visitorIdToBeLinked approved for prisoner - $prisonerId, request reference - $requestReference")
  }

  @Test
  fun `when approve visitor request is called and booker does not exist then NOT_FOUND status is returned`() {
    // Given
    val visitorIdToBeLinked = 12345L
    val bookerReference = "invalid-booker-ref"
    val prisonerId = "AB123456"
    val request = createVisitorRequest(bookerReference, prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REQUESTED)

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, request.reference, visitorId = visitorIdToBeLinked, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitorRequestsServiceSpy, times(1)).approveAndLinkVisitorRequest(request.reference, LinkVisitorRequestDto(visitorIdToBeLinked))
    verify(bookerDetailsServiceSpy, times(0)).createBookerPrisonerVisitor(any(), any(), any(), any())
    verify(bookerDetailsStoreServiceSpy, times(0)).storeBookerPrisonerVisitor(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).approveVisitorRequest(any(), any())
  }

  @Test
  fun `when approve visitor request is called and visitor request does not exist then NOT_FOUND status is returned`() {
    // Given
    val reference = "missingRef"
    val visitorIdToBeLinked = 12345L

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, reference, visitorId = visitorIdToBeLinked, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitorRequestsServiceSpy, times(1)).approveAndLinkVisitorRequest(reference, LinkVisitorRequestDto(visitorIdToBeLinked))
    verify(bookerDetailsServiceSpy, times(0)).createBookerPrisonerVisitor(any(), any(), any(), any())
    verify(bookerDetailsStoreServiceSpy, times(0)).storeBookerPrisonerVisitor(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).approveVisitorRequest(any(), any())
  }

  @Test
  fun `when approve visitor request is called but visitor already approved then NOT_FOUND status is returned`() {
    // Given
    // Given
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)
    val visitorIdToBeLinked = 12345L

    // request has already been APPROVED
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = APPROVED)

    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, request.reference, visitorId = visitorIdToBeLinked, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor request already approved")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor request with reference ${request.reference} has already been approved.")

    verify(visitorRequestsServiceSpy, times(1)).approveAndLinkVisitorRequest(request.reference, LinkVisitorRequestDto(visitorIdToBeLinked))
    verify(bookerDetailsServiceSpy, times(0)).createBookerPrisonerVisitor(any(), any(), any(), any())
    verify(bookerDetailsStoreServiceSpy, times(0)).storeBookerPrisonerVisitor(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).approveVisitorRequest(any(), any())
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = callApproveVisitorRequest(webTestClient, "visitorRequestRef", 12345L, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  fun callApproveVisitorRequest(
    webTestClient: WebTestClient,
    requestReference: String,
    visitorId: Long,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(APPROVE_VISITOR_REQUEST.replace("{requestReference}", requestReference))
    .body(BodyInserters.fromValue(LinkVisitorRequestDto(visitorId)))
    .headers(authHttpHeaders)
    .exchange()
}
