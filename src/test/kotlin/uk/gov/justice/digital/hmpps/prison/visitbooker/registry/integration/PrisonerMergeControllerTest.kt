package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.PRISONER_MERGE_BATCH_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.PRISONER_MERGE_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonerMergeBatchRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonerMergeRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@Transactional(propagation = SUPPORTS)
@DisplayName("Manual prisoner merge controller tests")
class PrisonerMergeControllerTest : IntegrationTestBase() {
  private lateinit var booker1: Booker
  private lateinit var booker2: Booker
  private lateinit var booker3: Booker

  @MockitoSpyBean
  private lateinit var telemetryClientSpy: TelemetryClient

  @MockitoSpyBean
  private lateinit var permittedPrisonerRepositorySpy: PermittedPrisonerRepository

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")
    booker3 = createBooker(oneLoginSub = "789", emailAddress = "test2@example.com")
  }

  @Test
  fun `when manual prisoner merge is requested then old prisoner number is updated with new prisoner number`() {
    // Given
    val oldPrisonerNumber = "AB123XYZ"
    val newPrisonerNumber = "BB123ABC"
    val otherPrisonerNumber = "OTH123"

    createAssociatedPrisoners(
      booker1,
      listOf(
        PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE),
        PermittedPrisonerTestObject(otherPrisonerNumber, PRISON_CODE),
      ),
    )
    createAssociatedPrisoners(booker2, listOf(PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE)))
    createAssociatedPrisoners(booker3, listOf(PermittedPrisonerTestObject(otherPrisonerNumber, PRISON_CODE)))

    val request = PrisonerMergeRequestDto(
      oldPrisonerNumber = oldPrisonerNumber,
      newPrisonerNumber = newPrisonerNumber,
    )

    // When
    val responseSpec = callMergePrisoner(bookerConfigServiceRoleHttpHeaders, request)

    // Then
    responseSpec.expectStatus().isOk

    assertThat(prisonerIdsForBooker(booker1)).containsExactlyInAnyOrder(newPrisonerNumber, otherPrisonerNumber)
    assertThat(prisonerIdsForBooker(booker2)).containsExactly(newPrisonerNumber)
    assertThat(prisonerIdsForBooker(booker3)).containsExactly(otherPrisonerNumber)
  }

  @Test
  fun `when manual prisoner merge is requested with event field names then old prisoner number is updated`() {
    // Given
    val oldPrisonerNumber = "AB123XYZ"
    val newPrisonerNumber = "BB123ABC"

    createAssociatedPrisoners(booker1, listOf(PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE)))

    val request = mapOf(
      "removedNomsNumber" to oldPrisonerNumber,
      "nomsNumber" to newPrisonerNumber,
    )

    // When
    val responseSpec = callMergePrisoner(bookerConfigServiceRoleHttpHeaders, request)

    // Then
    responseSpec.expectStatus().isOk

    assertThat(prisonerIdsForBooker(booker1)).containsExactly(newPrisonerNumber)
  }

  @Test
  fun `when manual prisoner merge batch is requested then each old prisoner number is updated`() {
    // Given
    val oldPrisonerNumber1 = "AB123XYZ"
    val newPrisonerNumber1 = "BB123ABC"
    val oldPrisonerNumber2 = "CB123XYZ"
    val newPrisonerNumber2 = "DB123ABC"

    createAssociatedPrisoners(
      booker1,
      listOf(
        PermittedPrisonerTestObject(oldPrisonerNumber1, PRISON_CODE),
        PermittedPrisonerTestObject(oldPrisonerNumber2, PRISON_CODE),
      ),
    )
    createAssociatedPrisoners(booker2, listOf(PermittedPrisonerTestObject(oldPrisonerNumber1, PRISON_CODE)))
    createAssociatedPrisoners(booker3, listOf(PermittedPrisonerTestObject(oldPrisonerNumber2, PRISON_CODE)))

    val request = PrisonerMergeBatchRequestDto(
      prisonerMerges = listOf(
        PrisonerMergeRequestDto(oldPrisonerNumber = oldPrisonerNumber1, newPrisonerNumber = newPrisonerNumber1),
        PrisonerMergeRequestDto(oldPrisonerNumber = oldPrisonerNumber2, newPrisonerNumber = newPrisonerNumber2),
      ),
    )

    // When
    val responseSpec = callMergePrisoners(bookerConfigServiceRoleHttpHeaders, request)

    // Then
    responseSpec.expectStatus().isOk

    assertThat(prisonerIdsForBooker(booker1)).containsExactlyInAnyOrder(newPrisonerNumber1, newPrisonerNumber2)
    assertThat(prisonerIdsForBooker(booker2)).containsExactly(newPrisonerNumber1)
    assertThat(prisonerIdsForBooker(booker3)).containsExactly(newPrisonerNumber2)
  }

  @Test
  fun `when manual prisoner merge batch fails halfway then failure is logged and remaining merges continue`() {
    // Given
    val successfulOldPrisonerNumber = "AB123XYZ"
    val successfulNewPrisonerNumber = "BB123ABC"
    val failingOldPrisonerNumber = "FAILOLD"
    val failingNewPrisonerNumber = "FAILNEW"
    val nextSuccessfulOldPrisonerNumber = "CB123XYZ"
    val nextSuccessfulNewPrisonerNumber = "DB123ABC"

    createAssociatedPrisoners(booker1, listOf(PermittedPrisonerTestObject(successfulOldPrisonerNumber, PRISON_CODE)))
    createAssociatedPrisoners(booker2, listOf(PermittedPrisonerTestObject(failingOldPrisonerNumber, PRISON_CODE)))
    createAssociatedPrisoners(booker3, listOf(PermittedPrisonerTestObject(nextSuccessfulOldPrisonerNumber, PRISON_CODE)))

    doThrow(RuntimeException("Batch merge failure"))
      .whenever(permittedPrisonerRepositorySpy)
      .mergePrisoner(oldPrisonerId = failingOldPrisonerNumber, newPrisonerId = failingNewPrisonerNumber)

    val request = PrisonerMergeBatchRequestDto(
      prisonerMerges = listOf(
        PrisonerMergeRequestDto(oldPrisonerNumber = successfulOldPrisonerNumber, newPrisonerNumber = successfulNewPrisonerNumber),
        PrisonerMergeRequestDto(oldPrisonerNumber = failingOldPrisonerNumber, newPrisonerNumber = failingNewPrisonerNumber),
        PrisonerMergeRequestDto(oldPrisonerNumber = nextSuccessfulOldPrisonerNumber, newPrisonerNumber = nextSuccessfulNewPrisonerNumber),
      ),
    )

    // When
    val responseSpec = callMergePrisoners(bookerConfigServiceRoleHttpHeaders, request)

    // Then
    responseSpec.expectStatus().isOk

    assertThat(prisonerIdsForBooker(booker1)).containsExactly(successfulNewPrisonerNumber)
    assertThat(prisonerIdsForBooker(booker2)).containsExactly(failingOldPrisonerNumber)
    assertThat(prisonerIdsForBooker(booker3)).containsExactly(nextSuccessfulNewPrisonerNumber)

    verify(telemetryClientSpy, times(1)).trackEvent(
      "booker_merge_event_failed",
      mapOf(
        "oldPrisonerNumber" to failingOldPrisonerNumber,
        "newPrisonerNumber" to failingNewPrisonerNumber,
        "exceptionType" to RuntimeException::class.java.name,
        "exceptionMessage" to "Batch merge failure",
      ),
      null,
    )
  }

  @Test
  fun `when manual prisoner merge is called with incorrect role then forbidden is returned`() {
    // Given
    val request = PrisonerMergeRequestDto(
      oldPrisonerNumber = "AB123XYZ",
      newPrisonerNumber = "BB123ABC",
    )

    // When
    val responseSpec = callMergePrisoner(orchestrationServiceRoleHttpHeaders, request)

    // Then
    responseSpec.expectStatus().isForbidden
  }

  @Test
  fun `when manual prisoner merge is called with invalid request then bad request is returned`() {
    // Given
    val request = PrisonerMergeRequestDto(
      oldPrisonerNumber = "",
      newPrisonerNumber = "BB123ABC",
    )

    // When
    val responseSpec = callMergePrisoner(bookerConfigServiceRoleHttpHeaders, request)

    // Then
    responseSpec.expectStatus().isBadRequest
  }

  private fun prisonerIdsForBooker(booker: Booker): List<String> = permittedPrisonerRepository.findByBookerId(booker.id).map { it.prisonerId }

  private fun callMergePrisoner(
    authHttpHeaders: (HttpHeaders) -> Unit,
    request: Any,
  ): ResponseSpec = webTestClient.post().uri(PRISONER_MERGE_PATH)
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(request))
    .exchange()

  private fun callMergePrisoners(
    authHttpHeaders: (HttpHeaders) -> Unit,
    request: PrisonerMergeBatchRequestDto,
  ): ResponseSpec = webTestClient.post().uri(PRISONER_MERGE_BATCH_PATH)
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(request))
    .exchange()
}
