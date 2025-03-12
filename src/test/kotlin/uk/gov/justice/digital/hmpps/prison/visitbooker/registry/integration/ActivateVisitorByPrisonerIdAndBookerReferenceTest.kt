package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.ACTIVATE_BOOKER_PRISONER_VISITOR_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.ACTIVATED_VISITOR
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@DisplayName("Activate booker prisoner visitor")
class ActivateVisitorByPrisonerIdAndBookerReferenceTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  private lateinit var booker: Booker

  private lateinit var prisoner: PermittedPrisoner

  @Autowired
  private lateinit var permittedVisitorRepository: PermittedVisitorRepository

  private lateinit var visitor1: PermittedVisitor
  private lateinit var visitor2: PermittedVisitor

  @BeforeEach
  internal fun setUp() {
    booker = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    val prisoners = createAssociatedPrisoners(
      booker,
      listOf(
        PermittedPrisonerTestObject("AB123456", PRISON_CODE, true),
        PermittedPrisonerTestObject("AB789012", PRISON_CODE, true),
      ),
      visitors = listOf(),
    )

    prisoner = prisoners[0]

    val visitors = createAssociatedPrisonersVisitors(
      prisoner,
      listOf(
        PermittedVisitorTestObject(12, false),
        PermittedVisitorTestObject(34, false),
      ),
    )
    visitor1 = visitors[0]
    visitor2 = visitors[1]
  }

  @Test
  fun `when prisoner is activated for booker all data is persisted and returned correctly`() {
    // Given

    // When
    val responseSpec = activateVisitorByPrisonerIsAndBookerReference(webTestClient, booker.reference, prisoner.prisonerId, visitor1.visitorId, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk
    val associatedPermittedVisitor = getResults(returnResult.expectBody())
    assertThat(associatedPermittedVisitor).isNotNull
    associatedPermittedVisitor?.let {
      assertThat(associatedPermittedVisitor.active).isTrue()
      val permittedVisitors = permittedVisitorRepository.findByPermittedPrisonerId(prisoner.id)
      assertThat(permittedVisitors.first { visitor1.id == it.id }.active).isTrue
      assertThat(permittedVisitors.first { visitor2.id == it.id }.active).isFalse

      verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
      verify(telemetryClientSpy, times(1)).trackEvent(
        ACTIVATED_VISITOR.telemetryType,
        mapOf(
          "bookerReference" to booker.reference,
          "prisonerId" to prisoner.prisonerId,
          "visitorId" to visitor1.visitorId.toString(),
        ),
        null,
      )

      val auditEvents = bookerAuditRepository.findAll()
      assertThat(auditEvents).hasSize(1)
      assertAuditEvent(auditEvents[0], booker.reference, ACTIVATED_VISITOR, "Visitor ID - ${visitor1.visitorId} activated for prisoner - ${prisoner.prisonerId}")
    }
  }

  @Test
  fun `when invalid booker reference then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = activateVisitorByPrisonerIsAndBookerReference(webTestClient, "invalid-reference", prisoner.prisonerId, visitor1.visitorId, bookerConfigServiceRoleHttpHeaders)
    // Then
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor for prisoner booker invalid-reference/AB123456/12 not found")
  }

  @Test
  fun `when invalid prison id then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = activateVisitorByPrisonerIsAndBookerReference(webTestClient, booker.reference, "invalid-prisoner-id", visitor1.visitorId, bookerConfigServiceRoleHttpHeaders)
    // Then
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor for prisoner booker ${booker.reference}/invalid-prisoner-id/12 not found")
  }

  @Test
  fun `when invalid visitor id then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = activateVisitorByPrisonerIsAndBookerReference(webTestClient, booker.reference, prisoner.prisonerId, 666, bookerConfigServiceRoleHttpHeaders)
    // Then
    responseSpec.expectStatus().isNotFound

    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Visitor not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Visitor for prisoner booker ${booker.reference}/AB123456/666 not found")
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    // When
    val responseSpec = activateVisitorByPrisonerIsAndBookerReference(webTestClient, booker.reference, prisonerId = prisoner.prisonerId, visitorId = 12, setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): PermittedVisitorDto? = objectMapper.readValue(returnResult.returnResult().responseBody, PermittedVisitorDto::class.java)

  fun activateVisitorByPrisonerIsAndBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    visitorId: Long,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = ACTIVATE_BOOKER_PRISONER_VISITOR_CONTROLLER_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)
      .replace("{visitorId}", visitorId.toString())

    return webTestClient.put().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
