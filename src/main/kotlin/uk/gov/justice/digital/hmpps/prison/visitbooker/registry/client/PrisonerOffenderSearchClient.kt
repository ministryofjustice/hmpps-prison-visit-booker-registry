package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils.ClientUtils.Companion.isNotFoundError
import java.time.Duration

@Component
class PrisonerOffenderSearchClient(
  @Qualifier("prisonerOffenderSearchWebClient") private val webClient: WebClient,
  @Value("\${prisoner.search.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerById(prisonerId: String): PrisonerDto {
    LOG.trace("getPrisonerById - $prisonerId on prisoner search")

    return getPrisonerByIdAsMono(prisonerId)
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("Failed to get prisoner with id - $prisonerId on prisoner search")
          Mono.error(e)
        } else {
          LOG.error("Prisoner with id - $prisonerId not found.")
          Mono.error { PrisonerForBookerNotFoundException("Prisoner with id - $prisonerId not found on prisoner search") }
        }
      }
      .blockOptional(apiTimeout).orElseThrow { PrisonerForBookerNotFoundException("Prisoner with id - $prisonerId not found on prisoner search") }
  }

  private fun getPrisonerByIdAsMono(prisonerId: String): Mono<PrisonerDto> = webClient.get().uri("/prisoner/$prisonerId")
    .retrieve()
    .bodyToMono()
}
