package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation.SUPPORTS
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.GET_BOOKER_AUDIT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerAuditDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@Transactional(propagation = SUPPORTS)
@DisplayName("Get booker audit by reference, test for $GET_BOOKER_AUDIT")
class GetBookerAuditTest : IntegrationTestBase() {

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    booker2 = createBooker(oneLoginSub = "999", emailAddress = "test1@example.com")
  }

  @Test
  fun `when booker has audit entries then all audit entries are returned`() {
    // When
    createBookerAudit(bookerReference = booker1.reference, auditType = BookerAuditType.BOOKER_CREATED, text = "booker 1 created")
    createBookerAudit(bookerReference = booker1.reference, auditType = BookerAuditType.PRISONER_REGISTERED, text = "prisoner registered to booker 1")
    createBookerAudit(bookerReference = booker1.reference, auditType = BookerAuditType.VISITOR_ADDED_TO_PRISONER, text = "visitor added to prisoner for booker 1")

    createBookerAudit(bookerReference = booker2.reference, auditType = BookerAuditType.BOOKER_CREATED, text = "booker 2 created")
    createBookerAudit(bookerReference = booker2.reference, auditType = BookerAuditType.PRISONER_REGISTERED, text = "prisoner registered to booker 2")

    val responseSpec = getBookerAudit(webTestClient, booker1.reference, orchestrationServiceRoleHttpHeaders)

    // Then
    val returnResult = responseSpec.expectStatus().isOk.expectBody()
    val bookerAuditList = getResults(returnResult)
    assertThat(bookerAuditList).hasSize(3)
    assertBookerAudit(bookerAuditList!![0], booker1.reference, BookerAuditType.BOOKER_CREATED, "booker 1 created")
    assertBookerAudit(bookerAuditList[1], booker1.reference, BookerAuditType.PRISONER_REGISTERED, "prisoner registered to booker 1")
    assertBookerAudit(bookerAuditList[2], booker1.reference, BookerAuditType.VISITOR_ADDED_TO_PRISONER, "visitor added to prisoner for booker 1")
  }

  @Test
  fun `when invalid reference then NOT_FOUND status is returned`() {
    // When
    val responseSpec = getBookerAudit(webTestClient, "invalid-reference", orchestrationServiceRoleHttpHeaders)
    responseSpec.expectStatus().isNotFound
    responseSpec
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo("Booker not found")
      .jsonPath("$.developerMessage")
      .isEqualTo("No audits found for booker, either booker doesn't exist or has no audits")
  }

  @Test
  fun `access forbidden when no role`() {
    // When
    val responseSpec = getBookerAudit(webTestClient, booker1.reference, setAuthorisation(roles = listOf()))
    responseSpec.expectStatus().isForbidden
  }

  private fun assertBookerAudit(bookerAudit: BookerAuditDto, expectedBookerReference: String, expectedAuditType: BookerAuditType, expectedText: String) {
    assertThat(bookerAudit.bookerReference).isEqualTo(expectedBookerReference)
    assertThat(bookerAudit.auditType).isEqualTo(expectedAuditType)
    assertThat(bookerAudit.text).isEqualTo(expectedText)
  }

  private fun getResults(returnResult: WebTestClient.BodyContentSpec): Array<BookerAuditDto>? = TestObjectMapper.mapper.readValue(returnResult.returnResult().responseBody, Array<BookerAuditDto>::class.java)

  fun getBookerAudit(
    webTestClient: WebTestClient,
    bookerReference: String,
    authHttpHeaders: (HttpHeaders) -> Unit,
  ): WebTestClient.ResponseSpec {
    val url = GET_BOOKER_AUDIT.replace("{bookerReference}", bookerReference)
    return webTestClient.get().uri(url)
      .headers(authHttpHeaders)
      .exchange()
  }
}
