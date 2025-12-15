package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
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
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.REJECT_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RejectVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.VISITOR_REQUEST_REJECTED_FOR_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestRejectionReason
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestRejectionReason.ALREADY_LINKED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestRejectionReason.REJECT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.APPROVED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REJECTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.SnsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsStoreService
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@DisplayName("Reject visitor request - $REJECT_VISITOR_REQUEST")
class RejectVisitorRequestTest : IntegrationTestBase() {

  @Autowired
  private lateinit var visitorRequestsRepository: VisitorRequestsRepository

  @MockitoSpyBean
  private lateinit var visitorRequestsServiceSpy: VisitorRequestsService

  @MockitoSpyBean
  private lateinit var visitorRequestsStoreServiceSpy: VisitorRequestsStoreService

  @MockitoSpyBean
  private lateinit var visitorRequestsRepositorySpy: VisitorRequestsRepository

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @MockitoSpyBean
  lateinit var snsService: SnsService

  @Test
  fun `when a visitor request is rejected the request is marked as REJECTED and a CREATED response is returned`() {
    // Given
    val rejectionReason = ALREADY_LINKED
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val bookerReference = booker.reference
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)
    val prisonerId = prisoner.prisonerId
    val request = createVisitorRequest(bookerReference, prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REQUESTED)
    val requestReference = request.reference

    // When
    val responseSpec = callRejectVisitorRequest(webTestClient, requestReference, rejectionReason, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isCreated.expectBody()
    val visitorRequestResponse = getResults(returnResult)
    assertVisitorRequest(visitorRequestResponse, request, booker)

    val visitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference)
    Assertions.assertThat(visitorRequest!!.status).isEqualTo(REJECTED)

    verify(visitorRequestsServiceSpy, times(1)).rejectVisitorRequest(requestReference, RejectVisitorRequestDto(rejectionReason))
    verify(visitorRequestsStoreServiceSpy, times(1)).rejectVisitorRequest(bookerReference, prisonerId, request.reference)
    verify(visitorRequestsRepositorySpy, times(1)).rejectVisitorRequest(any(), any())

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      eq(VISITOR_REQUEST_REJECTED_FOR_PRISONER.telemetryEventName),
      check {
        assertThat(it["bookerReference"]).isEqualTo(bookerReference)
        assertThat(it["prisonerId"]).isEqualTo(prisonerId)
        assertThat(it["rejectionReason"]).isEqualTo(rejectionReason.name)
        assertThat(it["requestReference"]).isEqualTo(requestReference)
      },
      isNull(),
    )
    verify(snsService, times(1)).sendVisitorRequestRejectedEvent(bookerReference, prisonerId, rejectionReason)

    verify(telemetryClientSpy, times(1)).trackEvent(
      eq("prison-visit-booker.visitor-rejected-domain-event"),
      check {
        assertThat(it["bookerReference"]).isEqualTo(bookerReference)
        assertThat(it["prisonerId"]).isEqualTo(prisonerId)
        assertThat(it["rejectionReason"]).isEqualTo(rejectionReason.name)
      },
      isNull(),
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], bookerReference, VISITOR_REQUEST_REJECTED_FOR_PRISONER, "Request reference - $requestReference rejected with rejection reason - $rejectionReason")
  }

  @Test
  fun `when reject visitor request is called and booker does not exist then NOT_FOUND status is returned`() {
    // Given
    val rejectionReason = REJECT
    val bookerReference = "invalid-booker-ref"
    val prisonerId = "AB123456"
    val request = createVisitorRequest(bookerReference, prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REQUESTED)

    // When
    val responseSpec = callRejectVisitorRequest(webTestClient, request.reference, rejectionReason, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitorRequestsServiceSpy, times(1)).rejectVisitorRequest(request.reference, RejectVisitorRequestDto(rejectionReason))
    verify(visitorRequestsStoreServiceSpy, times(1)).rejectVisitorRequest(bookerReference, prisonerId, request.reference)
    verify(visitorRequestsRepositorySpy, times(0)).rejectVisitorRequest(any(), any())
  }

  @Test
  fun `when reject visitor request is called and visitor request does not exist then NOT_FOUND status is returned`() {
    // Given
    val rejectionReason = REJECT
    val reference = "missingRef"

    // When
    val responseSpec = callRejectVisitorRequest(webTestClient, reference, rejectionReason, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitorRequestsServiceSpy, times(1)).rejectVisitorRequest(reference, RejectVisitorRequestDto(rejectionReason))
    verify(visitorRequestsStoreServiceSpy, times(0)).rejectVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).rejectVisitorRequest(any(), any())
  }

  @Test
  fun `when reject visitor request is called but visitor already approved then NOT_FOUND status is returned`() {
    // Given
    val rejectionReason = REJECT
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)

    // request has already been APPROVED
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = APPROVED)

    // When
    val responseSpec = callRejectVisitorRequest(webTestClient, request.reference, rejectionReason, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor request already actioned")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor request with reference ${request.reference} has already been actioned.")

    verify(visitorRequestsServiceSpy, times(1)).rejectVisitorRequest(request.reference, RejectVisitorRequestDto(rejectionReason))
    verify(visitorRequestsStoreServiceSpy, times(0)).rejectVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).rejectVisitorRequest(any(), any())
  }

  @Test
  fun `when reject visitor request is called but visitor already rejected then NOT_FOUND status is returned`() {
    // Given
    val rejectionReason = REJECT
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)

    // request has already been REJECTED
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REJECTED)

    // When
    val responseSpec = callRejectVisitorRequest(webTestClient, request.reference, rejectionReason, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor request already actioned")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor request with reference ${request.reference} has already been actioned.")

    verify(visitorRequestsServiceSpy, times(1)).rejectVisitorRequest(request.reference, RejectVisitorRequestDto(rejectionReason))
    verify(visitorRequestsStoreServiceSpy, times(0)).rejectVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).rejectVisitorRequest(any(), any())
  }

  @Test
  fun `access forbidden when no role`() {
    val rejectionReason = ALREADY_LINKED

    // When
    val responseSpec = callRejectVisitorRequest(webTestClient, "visitorRequestRef", rejectionReason, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonVisitorRequestDto = objectMapper.readValue(returnResult.returnResult().responseBody, PrisonVisitorRequestDto::class.java)

  fun callRejectVisitorRequest(
    webTestClient: WebTestClient,
    requestReference: String,
    rejectionReason: VisitorRequestRejectionReason,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(REJECT_VISITOR_REQUEST.replace("{requestReference}", requestReference))
    .body(BodyInserters.fromValue(RejectVisitorRequestDto(rejectionReason)))
    .headers(authHttpHeaders)
    .exchange()

  private fun assertVisitorRequest(visitorRequestResponse: PrisonVisitorRequestDto, visitorRequest: VisitorRequest, booker: Booker) {
    assertThat(visitorRequestResponse.reference).isEqualTo(visitorRequest.reference)
    assertThat(visitorRequestResponse.bookerReference).isEqualTo(booker.reference)
    assertThat(visitorRequestResponse.requestedOn).isEqualTo(visitorRequest.createTimestamp!!.toLocalDate())
    assertThat(visitorRequestResponse.prisonerId).isEqualTo(visitorRequest.prisonerId)
    assertThat(visitorRequestResponse.firstName).isEqualTo(visitorRequest.firstName)
    assertThat(visitorRequestResponse.lastName).isEqualTo(visitorRequest.lastName)
    assertThat(visitorRequestResponse.dateOfBirth).isEqualTo(visitorRequest.dateOfBirth)
    assertThat(visitorRequestResponse.bookerEmail).isEqualTo(booker.email)
  }
}
