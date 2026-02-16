package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils.ClientUtils.Companion.isNotFoundError
import java.time.Duration

@Component
class PrisonerContactRegistryClient(
  @param:Qualifier("prisonerContactRegistryWebClient")
  private val webClient: WebClient,
  @param:Value("\${prisoner-contact.registry.timeout:10s}")
  private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)

    const val CONTACT_REGISTRY_CONTACTS_PATH: String = "/v2/prisoners/{prisonerId}/contacts"
    const val CONTACT_REGISTRY_SOCIAL_CONTACTS_PATH: String = "$CONTACT_REGISTRY_CONTACTS_PATH/social"
  }

  fun getPrisonersSocialContacts(prisonerId: String): List<PrisonerContactDto> {
    val uri = CONTACT_REGISTRY_SOCIAL_CONTACTS_PATH.replace("{prisonerId}", prisonerId)

    return webClient.get()
      .uri(uri) { getSocialContactsUriBuilder(it).build() }
      .retrieve()
      .bodyToMono<List<PrisonerContactDto>>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getPrisonersSocialContacts Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getPrisonersSocialContacts NOT_FOUND for get request $uri")
          Mono.error { PrisonerNotFoundException("Social Contacts for prisonerId - $prisonerId not found on prisoner-contact-registry") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { IllegalStateException("no response from social contacts endpoint with uri $uri") }
  }

  private fun getSocialContactsUriBuilder(
    uriBuilder: UriBuilder,
  ): UriBuilder {
    uriBuilder.queryParam("hasDateOfBirth", false)
    uriBuilder.queryParam("withAddress", false)
    return uriBuilder
  }
}
