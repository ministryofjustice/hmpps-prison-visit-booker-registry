package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var authDetailRepository: AuthDetailRepository

  @Autowired
  protected lateinit var bookerRepository: BookerRepository

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @AfterEach
  @Transactional
  internal open fun deleteAll() {
    bookerRepository.deleteAll()
    authDetailRepository.deleteAll()
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)
}
