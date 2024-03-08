package uk.gov.justice.digital.hmpps.oneloginuserregistry.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.mock.HmppsAuthExtension

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  companion object {

    private val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
//        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
//        registry.add("spring.datasource.username", pgContainer::getUsername)
//        registry.add("spring.datasource.password", pgContainer::getPassword)
//        registry.add("spring.datasource.placeholders.database_update_password", pgContainer::getPassword)
//        registry.add("spring.datasource.placeholders.database_read_only_password", pgContainer::getPassword)
//        registry.add("spring.flyway.url", pgContainer::getJdbcUrl)
//        registry.add("spring.flyway.user", pgContainer::getUsername)
//        registry.add("spring.flyway.password", pgContainer::getPassword)
      }
    }
  }
}
