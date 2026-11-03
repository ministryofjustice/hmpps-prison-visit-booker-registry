package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.UNLINK_PRISONER_ENDPOINT_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ActionedByDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@DisplayName("Unlink booker prisoner - POST $UNLINK_PRISONER_ENDPOINT_PATH")
class UnlinkPrisonerFromBookerTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var bookerAuditRepositorySpy: BookerAuditRepository

  @MockitoSpyBean
  lateinit var telemetryClientSpy: TelemetryClient

  private lateinit var booker: Booker

  @Autowired
  private lateinit var permittedVisitorRepository: PermittedVisitorRepository
  val prisoner1Id = "AB123456"
  val prisoner2Id = "AB789012"

  @BeforeEach
  internal fun setUp() {
    booker = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    createAssociatedPrisoners(
      booker,
      listOf(
        PermittedPrisonerTestObject(prisoner1Id, PRISON_CODE),
        PermittedPrisonerTestObject(prisoner2Id, PRISON_CODE),
      ),
      visitors = listOf(
        PermittedVisitorTestObject(1),
        PermittedVisitorTestObject(2),
      ),
    )
  }

  @Test
  fun `when prisoner is unlinked for booker then they're removed from the DB successfully`() {
    // Given
    val actionedByDto = ActionedByDto(actionedBy = "test-user")

    // When
    val responseSpec = unlinkPrisonerIdAndBookerReference(webTestClient, booker.reference, prisoner1Id, actionedByDto, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    verify(bookerAuditRepositorySpy, times(1)).saveAndFlush(any<BookerAudit>())
    verify(telemetryClientSpy, times(1)).trackEvent(
      BookerAuditType.UNLINK_PRISONER.telemetryEventName,
      mapOf(
        "bookerReference" to booker.reference,
        "prisonerId" to prisoner1Id,
        "actionedBy" to actionedByDto.actionedBy,
      ),
      null,
    )

    val auditEvents = bookerAuditRepository.findAll()
    assertThat(auditEvents).hasSize(1)
    assertAuditEvent(auditEvents[0], booker.reference, BookerAuditType.UNLINK_PRISONER, "Prisoner ID - $prisoner1Id unlinked for booker - ${booker.reference}, actionedBy - ${actionedByDto.actionedBy}")

    val prisonersForBooker = permittedPrisonerRepository.findAll()
    assertThat(prisonersForBooker).hasSize(1)
    assertThat(prisonersForBooker[0].prisonerId).isNotEqualTo(prisoner1Id)
  }

  @Test
  fun `when prisoner id has different casing to stored prisoner then visitor is unlinked successfully`() {
    // Given
    val actionedByDto = ActionedByDto(actionedBy = "test-user")

    // When
    val responseSpec = unlinkPrisonerIdAndBookerReference(webTestClient, booker.reference, prisoner1Id.lowercase(), actionedByDto, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk

    val prisonersForBooker = permittedPrisonerRepository.findAll()
    assertThat(prisonersForBooker).hasSize(1)
  }

  @Test
  fun `when prisoner not linked to a booker then NOT_FOUND status is returned`() {
    // Given
    val unlinkedPrisonerId = "UNLINKED_PRISONER"
    val actionedByDto = ActionedByDto(actionedBy = "test-user")

    // When
    val responseSpec = unlinkPrisonerIdAndBookerReference(webTestClient, booker.reference, unlinkedPrisonerId, actionedByDto, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // Given
    val actionedByDto = ActionedByDto(actionedBy = "test-user")
    // When
    val responseSpec = unlinkPrisonerIdAndBookerReference(webTestClient, "invalid-reference", prisonerId = "IDontExist", actionedByDto, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isNotFound
    verify(telemetryClientSpy, times(0)).trackEvent(any(), any(), anyOrNull())
  }

  @Test
  fun `when blank actionedBy then BAD_REQUEST status is returned`() {
    // Given
    val actionedByDto = ActionedByDto(actionedBy = "")
    // When
    val responseSpec = unlinkPrisonerIdAndBookerReference(webTestClient, "invalid-reference", prisonerId = "IDontExist", actionedByDto, bookerConfigServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isBadRequest
    verify(telemetryClientSpy, times(0)).trackEvent(any(), any(), anyOrNull())
  }

  @Test
  fun `access forbidden when no role`() {
    // Given
    val actionedByDto = ActionedByDto(actionedBy = "test-user")
    // When
    val responseSpec = unlinkPrisonerIdAndBookerReference(webTestClient, booker.reference, prisonerId = prisoner1Id, actionedByDto, setAuthorisation(roles = listOf()))
    // Then
    responseSpec.expectStatus().isForbidden
  }

  fun unlinkPrisonerIdAndBookerReference(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    actionedByDto: ActionedByDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = UNLINK_PRISONER_ENDPOINT_PATH
      .replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)

    return webTestClient.post().uri(url)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(actionedByDto))
      .exchange()
  }
}
