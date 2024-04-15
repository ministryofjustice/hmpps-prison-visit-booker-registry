package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.AUTH_DETAILS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@Transactional(propagation = SUPPORTS)
@DisplayName("Migrate $AUTH_DETAILS_CONTROLLER_PATH")
class AuthDetailsControllerTest : IntegrationTestBase() {

  protected lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI"))
  }

  @Test
  fun `when auth details are submitted for the first time with matching booker in db a reference is returned and data auth is saved`() {
    // Given
    val authDetailsDto = AuthDetailDto("IamASub", "aled.evans@govt.com", "0123456789")
    val pilotBooker = bookerRepository.saveAndFlush(Booker(email = authDetailsDto.email))
    // When
    val responseSpec = callBookerAuth(roleVisitSchedulerHttpHeaders, authDetailsDto)

    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).isEqualTo(pilotBooker.reference)
    assertThat(pilotBooker.oneLoginSub).isNull()

    val updatedPilotBooker = bookerRepository.findByEmail(authDetailsDto.email)
    assertThat(updatedPilotBooker).isNotNull
    updatedPilotBooker?.let {
      assertThat(it.oneLoginSub).isEqualTo(authDetailsDto.oneLoginSub)
    }

    val authDetails = authDetailRepository.findByOneLoginSub(authDetailsDto.oneLoginSub)
    assertThat(authDetails).isNotNull
    authDetails?.let {
      assertThat(it.id).isGreaterThan(0)
      assertThat(it.oneLoginSub).isEqualTo(authDetailsDto.oneLoginSub)
      assertThat(it.email).isEqualTo(authDetailsDto.email)
      assertThat(it.phoneNumber).isEqualTo(authDetailsDto.phoneNumber)
      assertThat(it.count).isEqualTo(0)
    }
  }

  @Test
  fun `when auth details are submitted for the second time with matching booker in db a reference is returned and data is updated`() {
    // Given
    val originalEmail = "aled.evans@govt.com"
    val authDetailsDto = AuthDetailDto("IamASub", "gwyn.evans@govt.com", "0123456789")
    val pilotBooker = bookerRepository.saveAndFlush(Booker(email = originalEmail, oneLoginSub = authDetailsDto.oneLoginSub))
    authDetailRepository.saveAndFlush(AuthDetail(count = 0, oneLoginSub = "IamASub", email = originalEmail, phoneNumber = "999"))

    // When
    val responseSpec = callBookerAuth(roleVisitSchedulerHttpHeaders, authDetailsDto)
    // Then
    responseSpec.expectStatus().isOk
    val reference = getReference(responseSpec)
    assertThat(reference).isEqualTo(pilotBooker.reference)

    val updatedPilotBooker = bookerRepository.findByEmail(originalEmail)
    assertThat(updatedPilotBooker).isNotNull
    updatedPilotBooker?.let {
      assertThat(it.oneLoginSub).isEqualTo(authDetailsDto.oneLoginSub)
    }

    val authDetails = authDetailRepository.findByOneLoginSub(authDetailsDto.oneLoginSub)
    assertThat(authDetails).isNotNull
    authDetails?.let {
      assertThat(it.id).isGreaterThan(0)
      assertThat(it.oneLoginSub).isEqualTo(authDetailsDto.oneLoginSub)
      assertThat(it.email).isEqualTo(authDetailsDto.email)
      assertThat(it.phoneNumber).isEqualTo(authDetailsDto.phoneNumber)
      assertThat(it.count).isEqualTo(1)
    }
  }

  @Test
  fun `when auth details are submitted and booker is not matched then not found exception thrown`() {
    // Given
    val authDetailsDto = AuthDetailDto("IamASub", "aled.evans@govt.com", "0123456789")

    // When
    val responseSpec = callBookerAuth(roleVisitSchedulerHttpHeaders, authDetailsDto)

    // Then
    responseSpec
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Booker not found")
      .jsonPath("$.developerMessage").value(Matchers.startsWith("Booker for Email : aled.evans@govt.com not found"))
  }

  @Test
  fun `when auth details are submitted for second time and booker is not matched on sub then not found exception thrown`() {
    // Given
    val authDetailsDto = AuthDetailDto("IamASub", "aled.evans@govt.com", "0123456789")
    bookerRepository.saveAndFlush(Booker(email = authDetailsDto.email, oneLoginSub = "Othersub"))
    authDetailRepository.saveAndFlush(AuthDetail(count = 1, oneLoginSub = authDetailsDto.oneLoginSub, email = authDetailsDto.email, phoneNumber = "999"))

    // When
    val responseSpec = callBookerAuth(roleVisitSchedulerHttpHeaders, authDetailsDto)

    // Then
    responseSpec
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Booker not found")
      .jsonPath("$.developerMessage").value(Matchers.startsWith("Booker for sub : IamASub not found"))
  }

  @Test
  fun `when auth details are submitted with in correct role an is exception thrown`() {
    // Given
    val authDetailsDto = AuthDetailDto("IamASub", "aled.evans@govt.com", "0123456789")

    val roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_I_AM_A_HACKER"))

    // When
    val responseSpec = callBookerAuth(roleVisitSchedulerHttpHeaders, authDetailsDto)

    // Then
    responseSpec
      .expectStatus().isForbidden
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Access is forbidden")
      .jsonPath("$.developerMessage").value(Matchers.startsWith("Access Denied"))
  }

  protected fun callBookerAuth(
    authHttpHeaders: (HttpHeaders) -> Unit,
    authDetailDto: AuthDetailDto,
  ): ResponseSpec {
    return webTestClient.put().uri(AUTH_DETAILS_CONTROLLER_PATH)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(authDetailDto))
      .exchange()
  }

  protected fun getReference(responseSpec: ResponseSpec): String {
    var reference = ""
    responseSpec.expectBody()
      .jsonPath("$")
      .value<String> { json -> reference = json }
    return reference
  }
}
