package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${prisoner.search.url}")
  private val prisonSearchBaseUrl: String,

  @Value("\${prisoner.search.timeout}")
  private val prisonSearchTimeout: Duration,
) {
  private final val clientRegistrationId: String = "hmpps-apis"

  @Bean
  fun prisonerOffenderSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient =
    builder.authorisedWebClient(authorizedClientManager, registrationId = clientRegistrationId, url = prisonSearchBaseUrl, timeout = prisonSearchTimeout)
}
