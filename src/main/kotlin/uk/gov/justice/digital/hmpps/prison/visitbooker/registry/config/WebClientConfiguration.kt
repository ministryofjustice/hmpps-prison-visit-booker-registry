package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.service.GlobalPrincipalOAuth2AuthorizedClientService
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${prisoner.search.url}")
  private val prisonSearchBaseUrl: String,

  @param:Value("\${visit-scheduler.api.url}")
  private val visitSchedulerUrl: String,

  @param:Value("\${prisoner-contact.registry.url}")
  private val prisonerContactRegistryBaseUrl: String,

  @param:Value("\${api.timeout:10s}")
  private val apiTimeout: Duration,

  @param:Value("\${api.health.timeout:2s}")
  private val healthTimeout: Duration,
) {
  private final val clientRegistrationId: String = "hmpps-apis"

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      GlobalPrincipalOAuth2AuthorizedClientService(clientRegistrationRepository),
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun prisonerOffenderSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonSearchBaseUrl, authorizedClientManager, builder)

  @Bean
  fun visitSchedulerWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(visitSchedulerUrl, authorizedClientManager, builder)

  @Bean
  fun prisonerContactRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = getWebClient(prisonerContactRegistryBaseUrl, authorizedClientManager, builder)

  private fun getWebClient(
    url: String,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    url = url,
    registrationId = clientRegistrationId,
    timeout = apiTimeout,
  )

  @Bean
  fun objectMapper(): ObjectMapper = JsonMapper.builder().changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }.build()
}
