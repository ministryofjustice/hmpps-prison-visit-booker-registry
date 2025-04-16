package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.validate

import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.VALIDATE_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError.PRISONER_RELEASED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError.PRISONER_TRANSFERRED_SUPPORTED_PRISON
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError.PRISONER_TRANSFERRED_UNSUPPORTED_PRISON
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.PermittedPrisonerTestObject
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitSchedulerService

@Transactional(propagation = SUPPORTS)
@DisplayName("Validate prisoner added for a booker.")
class PrisonerValidateTest : IntegrationTestBase() {
  private lateinit var booker: Booker

  private lateinit var prisoner: PermittedPrisonerTestObject

  @MockitoSpyBean
  private lateinit var prisonerSearchService: PrisonerSearchService

  @MockitoSpyBean
  private lateinit var visitSchedulerService: VisitSchedulerService

  @BeforeEach
  internal fun setUp() {
    booker = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    prisoner = PermittedPrisonerTestObject("AB123456", PRISON_CODE, true)

    createAssociatedPrisoners(
      booker,
      listOf(prisoner),
    )
  }

  @Test
  fun `when prisoner is in the same prison as expected a success response is returned`() {
    // When
    val prisonerId = prisoner.prisonerId

    // prisoner is in the same prison as when registered
    val prisoner1OffenderDetails = createPrisonerDto(prisonerNumber = prisonerId, prisonId = prisoner.prisonCode, inOutStatus = null)
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisoner1OffenderDetails)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)

    // Then
    responseSpec.expectStatus().isOk
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
  }

  @Test
  fun `when prisoner is in different prison than when registered and inOutStatus as OUT validation fails with PRISONER_RELEASED as error code`() {
    // When
    val prisonerId = prisoner.prisonerId
    val prisoner1OffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "OUT", inOutStatus = "OUT")
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisoner1OffenderDetails)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(returnResult)
    Assertions.assertThat(errorResponse.validationError).isEqualTo(PRISONER_RELEASED.name)
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
  }

  @Test
  fun `when prisoner is in a different prison than when registered and inOutStatus is null and prison is supported validation fails with PRISONER_TRANSFERRED_SUPPORTED_PRISON as error code`() {
    // When
    visitSchedulerMockServer.stubGetSupportedPublicPrisons(listOf("HEI", "CFI"))
    val prisonerId = prisoner.prisonerId

    // new prison is a supported prison
    val prisoner1OffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "CFI", inOutStatus = null)
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisoner1OffenderDetails)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(returnResult)
    Assertions.assertThat(errorResponse.validationError).isEqualTo(PRISONER_TRANSFERRED_SUPPORTED_PRISON.name)
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
    verify(visitSchedulerService, times(1)).getSupportedPublicPrisons()
  }

  @Test
  fun `when prisoner is in different prison than when registered and inOutStatus as IN and prison is supported validation fails with PRISONER_TRANSFERRED_SUPPORTED_PRISON as error code`() {
    // When
    visitSchedulerMockServer.stubGetSupportedPublicPrisons(listOf("HEI", "CFI"))
    val prisonerId = prisoner.prisonerId

    // new prison is a supported prison
    val prisonerOffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "CFI", inOutStatus = "IN")
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(returnResult)
    Assertions.assertThat(errorResponse.validationError).isEqualTo(PRISONER_TRANSFERRED_SUPPORTED_PRISON.name)
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
    verify(visitSchedulerService, times(1)).getSupportedPublicPrisons()
  }

  @Test
  fun `when prisoner is in a different prison than when registered and inOutStatus is null and prison is unsupported validation fails with PRISONER_TRANSFERRED_UNSUPPORTED_PRISON as error code`() {
    // When
    visitSchedulerMockServer.stubGetSupportedPublicPrisons(listOf("HEI", "CFI"))
    val prisonerId = prisoner.prisonerId

    // new prison is an unsupported prison
    val prisoner1OffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "XYZ", inOutStatus = null)
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisoner1OffenderDetails)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(returnResult)
    Assertions.assertThat(errorResponse.validationError).isEqualTo(PRISONER_TRANSFERRED_UNSUPPORTED_PRISON.name)
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
    verify(visitSchedulerService, times(1)).getSupportedPublicPrisons()
  }

  @Test
  fun `when prisoner is in different prison than when registered and inOutStatus as IN and prison is unsupported validation fails with PRISONER_TRANSFERRED_UNSUPPORTED_PRISON as error code`() {
    // When
    visitSchedulerMockServer.stubGetSupportedPublicPrisons(listOf("HEI", "CFI"))
    val prisonerId = prisoner.prisonerId

    // new prison is a supported prison
    val prisonerOffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "XYZ", inOutStatus = "IN")
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(returnResult)
    Assertions.assertThat(errorResponse.validationError).isEqualTo(PRISONER_TRANSFERRED_UNSUPPORTED_PRISON.name)
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
    verify(visitSchedulerService, times(1)).getSupportedPublicPrisons()
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = validatePrisoner(webTestClient, "invalid-reference", prisoner.prisonerId, orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when invalid prisoner id then NOT_FOUND status is returned`() {
    // When
    val responseSpec = validatePrisoner(webTestClient, booker.reference, "invalid-prisoner", orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when valid prisoner but call to offender search returns NOT_FOUND then NOT_FOUND status is returned`() {
    // When
    val prisonerId = prisoner.prisonerId
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, null, HttpStatus.SC_NOT_FOUND)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
  }

  @Test
  fun `when valid prisoner but call to offender search returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR status is returned`() {
    // When
    val prisonerId = prisoner.prisonerId
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, null, HttpStatus.SC_INTERNAL_SERVER_ERROR)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `when call to visit scheduler returns NOT_FOUND then NOT_FOUND status is returned`() {
    // When
    val prisonerId = prisoner.prisonerId
    val prisonerOffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "XYZ", inOutStatus = "IN")
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails, HttpStatus.SC_NOT_FOUND)
    visitSchedulerMockServer.stubGetSupportedPublicPrisons(null)

    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)
    // Then
    val returnResult = responseSpec.expectStatus().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY)
    val errorResponse = getValidationErrorResponse(returnResult)
    Assertions.assertThat(errorResponse.validationError).isEqualTo(PRISONER_TRANSFERRED_UNSUPPORTED_PRISON.name)
    verify(prisonerSearchService, times(1)).getPrisoner(prisonerId)
    verify(visitSchedulerService, times(1)).getSupportedPublicPrisons()
  }

  @Test
  fun `when call to visit scheduler returns INTERNAL_SERVER_ERROR then INTERNAL_SERVER_ERROR status is returned`() {
    // When
    val prisonerId = prisoner.prisonerId
    val prisonerOffenderDetails = createPrisonerDto(prisonerNumber = prisoner.prisonerId, prisonId = "XYZ", inOutStatus = "IN")
    prisonOffenderSearchMockServer.stubGetPrisoner(prisonerId, prisonerOffenderDetails, HttpStatus.SC_NOT_FOUND)
    visitSchedulerMockServer.stubGetSupportedPublicPrisons(null, HttpStatus.SC_INTERNAL_SERVER_ERROR)
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisonerId, orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = validatePrisoner(webTestClient, booker.reference, prisoner.prisonerId, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  fun getValidationErrorResponse(responseSpec: WebTestClient.ResponseSpec): BookerPrisonerValidationErrorResponse = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerPrisonerValidationErrorResponse::class.java)

  fun validatePrisoner(
    webTestClient: WebTestClient,
    bookerReference: String,
    prisonerId: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = VALIDATE_PRISONER.replace("{bookerReference}", bookerReference).replace("{prisonerId}", prisonerId)
    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
