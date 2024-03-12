package uk.gov.justice.digital.hmpps.oneloginuserregistry.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.oneloginuserregistry.controller.AUTH_DETAILS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto

@Transactional(propagation = SUPPORTS)
@DisplayName("Migrate $AUTH_DETAILS_CONTROLLER_PATH")
class AuthDetailsControllerTest : IntegrationTestBase() {

  protected lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  @BeforeEach
  internal fun setUp() {
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ONE_LOGIN_USER_REGISTRY"))
  }

  @Test
  fun `when auth details are submitted then details are saved`() {
    // Given
    val authDetails = AuthDetailDto("IamAReference", "aled.evans@govt.com", "0123456789")

    // When
    val responseSpec = callSaveAuthDetails(roleVisitSchedulerHttpHeaders, authDetails)

    // Then
    responseSpec.expectStatus().isCreated
  }

  protected fun callSaveAuthDetails(
    authHttpHeaders: (HttpHeaders) -> Unit,
    authDetailDto: AuthDetailDto,
  ): ResponseSpec {
    return webTestClient.put().uri(AUTH_DETAILS_CONTROLLER_PATH)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(authDetailDto))
      .exchange()
  }
}
