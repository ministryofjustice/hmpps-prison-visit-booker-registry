package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.GET_BOOKER_USING_EMAIL
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@Transactional(propagation = SUPPORTS)
@DisplayName("Get booker by email")
class BookerByEmailTest : IntegrationTestBase() {

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  private lateinit var prisoner1: PermittedPrisonerTestObject
  private lateinit var prisoner2: PermittedPrisonerTestObject
  private lateinit var prisoner3: PermittedPrisonerTestObject

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has no permittedPrisoners associated
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    prisoner1 = PermittedPrisonerTestObject("AB123456", PRISON_CODE, true)
    prisoner2 = PermittedPrisonerTestObject("AB789012", PRISON_CODE, true)

    // inactive permittedPrisoner
    prisoner3 = PermittedPrisonerTestObject("AB345678", PRISON_CODE, false)

    createAssociatedPrisoners(
      booker1,
      listOf(prisoner1, prisoner2, prisoner3),
    )
  }

  @Test
  fun `get booker by email`() {
    // When
    val responseSpec = getBookerByEmail(webTestClient, booker1.email, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val booker = getResults(returnResult)
    assertThat(booker.permittedPrisoners).hasSize(3)
    assertThat(booker.permittedPrisoners[0].prisonerId).isEqualTo(prisoner1.prisonerId)
    assertThat(booker.permittedPrisoners[1].prisonerId).isEqualTo(prisoner2.prisonerId)
    assertThat(booker.permittedPrisoners[2].prisonerId).isEqualTo(prisoner3.prisonerId)
  }

  @Test
  fun `when invalid email then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getBookerByEmail(webTestClient, "invalid-email", bookerConfigServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Booker not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("Booker for email : invalid-email not found")
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getBookerByEmail(webTestClient, booker1.reference, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): BookerDto {
    return objectMapper.readValue(returnResult.returnResult().responseBody, BookerDto::class.java)
  }

  fun getBookerByEmail(
    webTestClient: WebTestClient,
    emailAddress: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = GET_BOOKER_USING_EMAIL.replace("{emailAddress}", emailAddress)
    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
