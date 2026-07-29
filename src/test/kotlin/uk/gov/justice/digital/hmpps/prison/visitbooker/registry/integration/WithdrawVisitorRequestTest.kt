package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.WITHDRAW_VISITOR_REQUEST
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.WithdrawVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.VISITOR_REQUEST_WITHDRAWN_FOR_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.APPROVED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.WITHDRAWN
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
@DisplayName("Withdraw visitor request - $WITHDRAW_VISITOR_REQUEST")
class WithdrawVisitorRequestTest : IntegrationTestBase() {
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
  fun `when a visitor request is withdrawn the request is marked as WITHDRAWN and an OK response is returned`() {
    // Given
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val bookerReference = booker.reference
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)
    val prisonerId = prisoner.prisonerId
    val request = createVisitorRequest(bookerReference, prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REQUESTED)
    val requestReference = request.reference

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, requestReference, booker.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val visitorRequestResponse = getResults(returnResult)
    assertVisitorRequest(visitorRequestResponse, request, booker)

    val visitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference)
    assertThat(visitorRequest!!.status).isEqualTo(WITHDRAWN)

    verify(visitorRequestsServiceSpy, times(1)).withdrawVisitorRequest(requestReference, WithdrawVisitorRequestDto(bookerReference = booker.reference))
    verify(visitorRequestsStoreServiceSpy, times(1)).withdrawVisitorRequest(bookerReference, prisonerId, request.reference)
    verify(visitorRequestsRepositorySpy, times(1)).withdrawVisitorRequest(any(), any())

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      eq(VISITOR_REQUEST_WITHDRAWN_FOR_PRISONER.telemetryEventName),
      check {
        assertThat(it["bookerReference"]).isEqualTo(bookerReference)
        assertThat(it["prisonerId"]).isEqualTo(prisonerId)
        assertThat(it["requestReference"]).isEqualTo(requestReference)
      },
      isNull(),
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(
      auditEvents[0],
      bookerReference,
      VISITOR_REQUEST_WITHDRAWN_FOR_PRISONER,
      "Request reference - ${request.reference} withdrawn, bookerReference - $bookerReference",
    )
  }

  @Test
  fun `when withdraw visitor request is called and visitor request does not exist then NOT_FOUND status is returned`() {
    // Given
    val reference = "missingRef"
    val userName = "test-user"

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, reference, userName, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(visitorRequestsServiceSpy, times(1)).withdrawVisitorRequest(reference, WithdrawVisitorRequestDto(bookerReference = userName))
    verify(visitorRequestsStoreServiceSpy, times(0)).withdrawVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).withdrawVisitorRequest(any(), any())
  }

  @Test
  fun `when withdraw visitor request is called but visitor already approved then BAD_REQUEST status is returned`() {
    // Given
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)

    // request has already been APPROVED
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = APPROVED)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, request.reference, booker.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor request already actioned")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor request with reference ${request.reference} has already been actioned, and cannot be withdrawn.")

    verify(visitorRequestsServiceSpy, times(1)).withdrawVisitorRequest(request.reference, WithdrawVisitorRequestDto(bookerReference = booker.reference))
    verify(visitorRequestsStoreServiceSpy, times(0)).withdrawVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).withdrawVisitorRequest(any(), any())
  }

  @Test
  fun `when withdraw visitor request is called but visitor already withdrawn then BAD_REQUEST status is returned`() {
    // Given
    val prisonCode = "HEI"
    val booker = createBooker("one-sub", "test@test.com")
    val prisoner = createPrisoner(booker, "AA123456", prisonCode)

    // request has already been WITHDRAWN
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = WITHDRAWN)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, request.reference, booker.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor request already actioned")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor request with reference ${request.reference} has already been actioned, and cannot be withdrawn.")

    verify(visitorRequestsServiceSpy, times(1)).withdrawVisitorRequest(
      request.reference,
      WithdrawVisitorRequestDto(bookerReference = booker.reference),
    )
    verify(visitorRequestsStoreServiceSpy, times(0)).withdrawVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).withdrawVisitorRequest(any(), any())
  }

  @Test
  fun `when withdraw visitor request is called but not by the original booker then NOT_FOUND status is returned`() {
    // Given
    val prisonCode = "HEI"

    val booker = createBooker("one-sub", "test@test.com")
    val otherBooker = createBooker("one-sub-2", "test2@test.com")

    val prisoner = createPrisoner(booker, "AA123456", prisonCode)

    // Request has been created by booker
    val request = createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstName1", "lastName1", LocalDate.now().minusYears(21)), status = REQUESTED)

    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, request.reference, otherBooker.reference, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor request not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor request with reference ${request.reference} not found for booker ${booker.reference}.")

    verify(visitorRequestsServiceSpy, times(1)).withdrawVisitorRequest(
      request.reference,
      WithdrawVisitorRequestDto(bookerReference = otherBooker.reference),
    )
    verify(visitorRequestsStoreServiceSpy, times(0)).withdrawVisitorRequest(any(), any(), any())
    verify(visitorRequestsRepositorySpy, times(0)).withdrawVisitorRequest(any(), any())
  }

  @Test
  fun `access forbidden when no role`() {
    val userName = "TEST-USER"
    // When
    val responseSpec = callWithdrawVisitorRequest(webTestClient, "visitorRequestRef", userName, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PrisonVisitorRequestDto = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, PrisonVisitorRequestDto::class.java)

  fun callWithdrawVisitorRequest(
    webTestClient: WebTestClient,
    requestReference: String,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri(WITHDRAW_VISITOR_REQUEST.replace("{requestReference}", requestReference))
    .body(BodyInserters.fromValue(WithdrawVisitorRequestDto(bookerReference = bookerReference)))
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
