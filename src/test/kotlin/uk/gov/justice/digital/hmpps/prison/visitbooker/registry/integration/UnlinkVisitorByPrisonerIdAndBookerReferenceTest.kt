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
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.VISITOR_ENDPOINT_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@DisplayName("Unlink booker prisoner visitor - DELETE $VISITOR_ENDPOINT_PATH")
class UnlinkVisitorByPrisonerIdAndBookerReferenceTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  private lateinit var booker: Booker
  private lateinit var bookerTwo: Booker

  private lateinit var prisoner: PermittedPrisoner
  private lateinit var prisonerForBookerTwo: PermittedPrisoner

  @Autowired
  private lateinit var permittedVisitorRepository: PermittedVisitorRepository

  private lateinit var visitor1: PermittedVisitor
  private lateinit var visitor2: PermittedVisitor
  private lateinit var visitor3: PermittedVisitor
  private lateinit var visitor4: PermittedVisitor

  @BeforeEach
  internal fun setUp() {
    booker = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    val prisoners = createAssociatedPrisoners(
      booker,
      listOf(
        PermittedPrisonerTestObject("AB123456", PRISON_CODE),
        PermittedPrisonerTestObject("AB789012", PRISON_CODE),
      ),
      visitors = listOf(),
    )

    prisoner = prisoners[0]

    val visitors = createAssociatedPrisonersVisitors(
      prisoner,
      listOf(
        PermittedVisitorTestObject(12),
        PermittedVisitorTestObject(34),
      ),
    )
    visitor1 = visitors[0]
    visitor2 = visitors[1]

    bookerTwo = createBooker(oneLoginSub = "987", emailAddress = "test-two@example.com")
    val prisonersForBookerTwo = createAssociatedPrisoners(
      booker,
      listOf(
        PermittedPrisonerTestObject("AB123456", PRISON_CODE),
        PermittedPrisonerTestObject("DD948472", PRISON_CODE),
      ),
      visitors = listOf(),
    )
    prisonerForBookerTwo = prisonersForBookerTwo[0]

    val visitorsForBookerTwo = createAssociatedPrisonersVisitors(
      prisonerForBookerTwo,
      listOf(
        PermittedVisitorTestObject(99),
        PermittedVisitorTestObject(100),
      ),
    )
    visitor3 = visitorsForBookerTwo[0]
    visitor4 = visitorsForBookerTwo[1]
  }

  @Test
  fun `when prisoner is unlinked for booker, then they're removed from the DB successfully`() {
    // Given

    // When
    val responseSpec = unlinkVisitorByPrisonerIdAndBookerReference(webTestClient, booker.reference, prisoner.prisonerId, visitor1.visitorId, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      BookerAuditType.UNLINK_VISITOR.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to prisoner.prisonerId,
        "visitorId" to visitor1.visitorId.toString(),
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, BookerAuditType.UNLINK_VISITOR, "Visitor ID - ${visitor1.visitorId} unlinked for prisoner - ${prisoner.prisonerId}, booker - ${booker.reference}")

    val visitors = permittedVisitorRepository.findAll()
    assertThat(visitors).hasSize(3)
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // Given
    // When
    val responseSpec = unlinkVisitorByPrisonerIdAndBookerReference(webTestClient, "invalid-reference", prisonerId = "IDontExist", visitorId = 123, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    // When
    val responseSpec = unlinkVisitorByPrisonerIdAndBookerReference(webTestClient, booker.reference, prisonerId = prisoner.prisonerId, visitorId = 12, setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }

  fun unlinkVisitorByPrisonerIdAndBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    visitorId: Long,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = VISITOR_ENDPOINT_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)
      .replace("{visitorId}", visitorId.toString())

    return webTestClient.delete().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
