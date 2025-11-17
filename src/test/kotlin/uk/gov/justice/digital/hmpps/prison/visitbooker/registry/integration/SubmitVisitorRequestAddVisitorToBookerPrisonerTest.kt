package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.PUBLIC_BOOKER_PRISONER_VISITOR_REQUESTS_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import java.time.LocalDate

@Transactional(propagation = SUPPORTS)
@TestPropertySource(properties = ["visitor-requests.request-limit=6"])
@DisplayName("Visitor Requests - $PUBLIC_BOOKER_PRISONER_VISITOR_REQUESTS_PATH")
class SubmitVisitorRequestAddVisitorToBookerPrisonerTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var visitorRequestsRepository: VisitorRequestsRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  @Test
  fun `when visitor request comes in, it is saved to database successfully`() {
    // Given
    val booker = createBooker(oneLoginSub = "123", emailAddress = "test@test.come")
    val prisoner = createPrisoner(booker, "AA123456")
    val visitorRequestDto = AddVisitorToBookerPrisonerRequestDto(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    // When
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisoner.prisonerId, listOf(PrisonerContactDto(personId = 543L, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21))))
    val responseSpec = callSubmitVisitorRequest(bookerConfigServiceRoleHttpHeaders, booker.reference, prisoner.prisonerId, visitorRequestDto)

    // Then
    responseSpec.expectStatus().isCreated

    verify(telemetryClientSpy, times(1)).trackEvent(
      BookerAuditType.VISITOR_REQUEST_SUBMITTED.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to prisoner.prisonerId,
      ),
      null,
    )
    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, BookerAuditType.VISITOR_REQUEST_SUBMITTED, "Booker ${booker.reference}, submitted request to add visitor to prisoner ${prisoner.prisonerId}")

    val visitorRequests = visitorRequestsRepository.findAll()
    assertThat(visitorRequests).hasSize(1)
    assertVisitorRequest(visitorRequests[0], booker.reference, prisoner.prisonerId, visitorRequestDto)
  }

  @Test
  fun `when visitor request comes in, but prisoner doesn't exist for booker, then validation response is returned`() {
    // Given
    val booker = createBooker(oneLoginSub = "123", emailAddress = "test@test.come")
    val prisonerId = "AA123456"
    val visitorRequestDto = AddVisitorToBookerPrisonerRequestDto(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    // When
    val responseSpec = callSubmitVisitorRequest(bookerConfigServiceRoleHttpHeaders, booker.reference, prisonerId, visitorRequestDto)

    // Then
    responseSpec.expectStatus().is4xxClientError

    verifyNoInteractions(telemetryClientSpy)

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)

    val visitorRequests = visitorRequestsRepository.findAll()
    assertThat(visitorRequests).hasSize(0)
  }

  @Test
  fun `when visitor request comes in, but booker has maximum amount of in progress requests, then validation response is returned`() {
    // Given
    val booker = createBooker(oneLoginSub = "123", emailAddress = "test@test.come")
    val prisoner = createPrisoner(booker, "AA123456")
    createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstA", "lastA", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)
    createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstB", "lastB", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)
    createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstC", "lastC", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)
    createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstD", "lastD", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)
    createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstE", "lastE", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)
    createVisitorRequest(booker.reference, prisoner.prisonerId, AddVisitorToBookerPrisonerRequestDto("firstF", "lastF", LocalDate.now().minusYears(21)), status = VisitorRequestsStatus.REQUESTED)

    val visitorRequestDto = AddVisitorToBookerPrisonerRequestDto(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    // When
    val responseSpec = callSubmitVisitorRequest(bookerConfigServiceRoleHttpHeaders, booker.reference, prisoner.prisonerId, visitorRequestDto)

    // Then
    responseSpec.expectStatus().is4xxClientError

    verifyNoInteractions(telemetryClientSpy)

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)

    val visitorRequests = visitorRequestsRepository.findAll()
    assertThat(visitorRequests).hasSize(6)
  }

  @Test
  fun `when visitor request comes in, if visitor is already linked, then validation response is returned`() {
    // Given
    val booker = createBooker(oneLoginSub = "123", emailAddress = "test@test.come")
    val prisoner = createPrisoner(booker, "AA123456")
    val visitor = createVisitor(prisoner, 123L)
    val visitorRequestDto = AddVisitorToBookerPrisonerRequestDto(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    // When
    prisonerContactRegistryMockServer.stubGetPrisonerContacts(prisonerId = prisoner.prisonerId, listOf(PrisonerContactDto(personId = visitor.visitorId, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21))))
    val responseSpec = callSubmitVisitorRequest(bookerConfigServiceRoleHttpHeaders, booker.reference, prisoner.prisonerId, visitorRequestDto)

    // Then
    responseSpec.expectStatus().is4xxClientError

    verifyNoInteractions(telemetryClientSpy)

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(0)

    val visitorRequests = visitorRequestsRepository.findAll()
    assertThat(visitorRequests).hasSize(0)
  }

  @Test
  fun `when end point is call with incorrect role then forbidden is returned`() {
    // Given
    val bookerReference = "abc-def-ghi"
    val prisonerId = "AA123456"
    val visitorRequestDto = AddVisitorToBookerPrisonerRequestDto(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )
    // When
    val responseSpec = callSubmitVisitorRequest(setAuthorisation(roles = listOf()), bookerReference, prisonerId, visitorRequestDto)

    // Then
    responseSpec
      .expectStatus().isForbidden
  }

  private fun callSubmitVisitorRequest(
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
    prisonerId: String,
    addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto,
  ): ResponseSpec {
    val uri = PUBLIC_BOOKER_PRISONER_VISITOR_REQUESTS_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)

    return webTestClient.post().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(addVisitorToBookerPrisonerRequestDto))
      .exchange()
  }

  private fun assertVisitorRequest(visitorRequestEntity: VisitorRequest, bookerReference: String, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto) {
    assertThat(visitorRequestEntity.bookerReference).isEqualTo(bookerReference)
    assertThat(visitorRequestEntity.prisonerId).isEqualTo(prisonerId)
    assertThat(visitorRequestEntity.firstName).isEqualTo(visitorRequest.firstName)
    assertThat(visitorRequestEntity.lastName).isEqualTo(visitorRequest.lastName)
    assertThat(visitorRequestEntity.dateOfBirth).isEqualTo(visitorRequest.dateOfBirth)
    assertThat(visitorRequestEntity.status).isEqualTo(VisitorRequestsStatus.REQUESTED)
  }
}
