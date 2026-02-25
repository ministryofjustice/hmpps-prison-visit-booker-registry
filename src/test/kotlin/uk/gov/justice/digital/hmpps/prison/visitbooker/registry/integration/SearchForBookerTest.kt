package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin.SEARCH_FOR_BOOKER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.SearchBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@Transactional(propagation = SUPPORTS)
@DisplayName("Search for booker(s) tests - $SEARCH_FOR_BOOKER")
class SearchForBookerTest : IntegrationTestBase() {

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  private lateinit var prisoner1: PermittedPrisonerTestObject
  private lateinit var prisoner2: PermittedPrisonerTestObject

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")

    // booker 2 has no permittedPrisoners associated
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")

    prisoner1 = PermittedPrisonerTestObject("AB123456", PRISON_CODE)
    prisoner2 = PermittedPrisonerTestObject("AB789012", PRISON_CODE)

    createAssociatedPrisoners(
      booker1,
      listOf(prisoner1, prisoner2),
    )
  }

  @Test
  fun `search for booker`() {
    // When
    val searchBookerDto = SearchBookerDto(email = booker1.email)
    val responseSpec = callSearchForBooker(webTestClient, searchBookerDto, bookerConfigServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val booker = getResults(returnResult).first()
    assertThat(booker.createdTimestamp).isNotNull()
    assertThat(booker.permittedPrisoners).hasSize(2)

    val actualPrisonerIds = booker.permittedPrisoners.map { it.prisonerId }.toSet()
    val expectedPrisonerIds = setOf(prisoner1.prisonerId, prisoner2.prisonerId)

    assertThat(actualPrisonerIds).isEqualTo(expectedPrisonerIds)
  }

  @Test
  fun `when invalid email then error status is returned`() {
    // When
    val searchBookerDto = SearchBookerDto(email = "")
    val responseSpec = callSearchForBooker(webTestClient, searchBookerDto, bookerConfigServiceRoleHttpHeaders)
    responseSpec.expectStatus().isBadRequest
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val searchBookerDto = SearchBookerDto(email = booker1.email)
    val responseSpec = callSearchForBooker(webTestClient, searchBookerDto, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): List<BookerDto> = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<BookerDto>::class.java).toList()

  fun callSearchForBooker(
    webTestClient: WebTestClient,
    searchBookerDto: SearchBookerDto,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): ResponseSpec = webTestClient.post().uri(SEARCH_FOR_BOOKER)
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(searchBookerDto))
    .exchange()
}
