package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.AUTH_DETAILS_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CLEAR_BOOKER_CONFIG_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePrisonerDto

@Transactional(propagation = SUPPORTS)
@DisplayName("Migrate $AUTH_DETAILS_CONTROLLER_PATH")
class BookerDetailsConfigControllerTest : IntegrationTestBase() {
  @Test
  fun `when booker dose not exist then booker is created with all child objects`() {
    // Given
    val visitorIds = listOf(1L, 2L)
    val prisoners = listOf(CreatePrisonerDto(prisonerId = "1233", visitorIds = visitorIds))
    val createBookerDto = CreateBookerDto(email = "aled@aled.com", prisoners = prisoners)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    responseSpec.expectStatus().isOk
    val dto = getBookerDto(responseSpec)

    assertThat(dto.reference).isNotNull()
    assertThat(dto.email).isEqualTo(createBookerDto.email)
    assertThat(dto.prisoners[0].prisonerId).isEqualTo(prisoners[0].prisonerId)
    assertThat(dto.prisoners[0].active).isTrue()
    assertThat(dto.prisoners[0].visitors.size).isEqualTo(visitorIds.size)
    visitorIds.forEachIndexed { index, visitorId ->
      dto.prisoners[0].visitors[index].let {
        assertThat(it.visitorId).isEqualTo(visitorId)
        assertThat(it.active).isTrue()
      }
    }
  }

  @Test
  fun `when booker dose exist then only new booker child objects are created created`() {
    // Given
    val emailAddress = "aled@aled.com"
    val oneLoginSub = "123"
    val booker = createBooker(oneLoginSub = oneLoginSub, emailAddress = emailAddress)
    val prisoner = createPrisoner(booker, prisonerId = "IM GONE")
    booker.prisoners.add(prisoner)
    val visitor = createVisitor(prisoner, visitorId = 0L)
    prisoner.visitors.add(visitor)
    bookerRepository.saveAndFlush(booker)

    val visitorIds = listOf(1L, 2L)
    val prisoners = listOf(CreatePrisonerDto(prisonerId = "1235", visitorIds = visitorIds))
    val createBookerDto = CreateBookerDto(email = "aled@aled.com", prisoners = prisoners)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    responseSpec.expectStatus().isOk
    val dto = getBookerDto(responseSpec)
    assertThat(dto.reference).isEqualTo(booker.reference)
    assertThat(dto.oneLoginSub).isEqualTo(booker.oneLoginSub)
    assertThat(dto.email).isEqualTo(booker.email)
    assertThat(dto.email).isEqualTo(createBookerDto.email)
    assertThat(dto.prisoners[0].prisonerId).isEqualTo(prisoners[0].prisonerId)
    assertThat(dto.prisoners[0].active).isTrue()
    assertThat(dto.prisoners[0].visitors.size).isEqualTo(visitorIds.size)
    visitorIds.forEachIndexed { index, visitorId ->
      dto.prisoners[0].visitors[index].let {
        assertThat(it.visitorId).isEqualTo(visitorId)
        assertThat(it.active).isTrue()
      }
    }
  }

  @Test
  fun `when booker email is not given exception is thrown`() {
    // Given
    val visitorIds = listOf(1L, 2L)
    val prisoners = listOf(CreatePrisonerDto(prisonerId = "1233", visitorIds = visitorIds))
    val createBookerDto = CreateBookerDto(email = "", prisoners = prisoners)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    assertError(responseSpec, "Invalid Argument: email", "must not be blank")
  }

  @Test
  fun `when booker prisoners are not given exception is thrown`() {
    // Given
    val createBookerDto = CreateBookerDto(email = "aled@aled.com", prisoners = listOf())

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    assertError(responseSpec, "Invalid Argument: prisoners", "must not be empty")
  }

  @Test
  fun `when booker prisonerId is not given exception is thrown`() {
    // Given
    val visitorIds = listOf(1L, 2L)
    val prisoners = listOf(CreatePrisonerDto(prisonerId = "", visitorIds = visitorIds))
    val createBookerDto = CreateBookerDto(email = "aled@aled.com", prisoners = prisoners)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    assertError(responseSpec, "Invalid Argument: prisoners[0].prisonerId", "must not be blank")
  }

  @Test
  fun `when booker visitors are not given exception is thrown`() {
    // Given
    val prisoners = listOf(CreatePrisonerDto(prisonerId = "1233", visitorIds = listOf()))
    val createBookerDto = CreateBookerDto(email = "aled@aled.com", prisoners = prisoners)

    // When
    val responseSpec = callCreateBooker(bookerConfigServiceRoleHttpHeaders, createBookerDto)

    // Then
    assertError(responseSpec, "Invalid Argument: prisoners[0].visitorIds", "must not be empty")
  }

  @Test
  fun `when booker end point is call with incorrect role`() {
    // Given
    val visitorIds = listOf(1L, 2L)
    val prisoners = listOf(CreatePrisonerDto(prisonerId = "1", visitorIds = visitorIds))
    val createBookerDto = CreateBookerDto(email = "aled@aled.com", prisoners = prisoners)

    // When
    val responseSpec = callCreateBooker(orchestrationServiceRoleHttpHeaders, createBookerDto)

    // Then
    responseSpec
      .expectStatus().isForbidden
  }

  @Test
  fun `when booker details are cleared all child objects are removed`() {
    // Given
    val emailAddress = "aled@aled.com"
    val oneLoginSub = "123"
    val booker = createBooker(oneLoginSub = oneLoginSub, emailAddress = emailAddress)
    val prisoner = createPrisoner(booker, prisonerId = "IM GONE")
    booker.prisoners.add(prisoner)
    val visitor = createVisitor(prisoner, visitorId = 0L)
    prisoner.visitors.add(visitor)
    bookerRepository.saveAndFlush(booker)

    // When
    val responseSpec = callClearBookerDetails(bookerConfigServiceRoleHttpHeaders, booker.reference)

    // Then
    responseSpec.expectStatus().isOk
    val dto = getBookerDto(responseSpec)
    assertThat(dto.email).isEqualTo(booker.email)
    assertThat(dto.reference).isEqualTo(booker.reference)
    assertThat(dto.oneLoginSub).isEqualTo(booker.oneLoginSub)
    assertThat(dto.prisoners).isEmpty()

    val savedBooker = bookerRepository.findByReference(booker.reference)
    assertThat(savedBooker).isNotNull
    savedBooker?.let {
      assertThat(savedBooker.email).isEqualTo(booker.email)
      assertThat(savedBooker.reference).isEqualTo(booker.reference)
      assertThat(savedBooker.oneLoginSub).isEqualTo(booker.oneLoginSub)
      assertThat(savedBooker.prisoners).isEmpty()
    }
  }

  protected fun callCreateBooker(
    authHttpHeaders: (HttpHeaders) -> Unit,
    createBookerDto: CreateBookerDto,
  ): ResponseSpec {
    return webTestClient.put().uri(PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(createBookerDto))
      .exchange()
  }

  protected fun callClearBookerDetails(
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): ResponseSpec {
    return webTestClient.delete().uri(CLEAR_BOOKER_CONFIG_CONTROLLER_PATH.replace("{bookerReference}", bookerReference))
      .headers(authHttpHeaders)
      .exchange()
  }

  protected fun getBookerDto(responseSpec: ResponseSpec): BookerDto {
    return objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerDto::class.java)
  }

  private fun assertError(responseSpec: ResponseSpec, userMessage: String, developerMessage: String) {
    responseSpec
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").value(Matchers.equalTo(userMessage))
      .jsonPath("$.developerMessage").value(Matchers.containsString(developerMessage))
  }
}
