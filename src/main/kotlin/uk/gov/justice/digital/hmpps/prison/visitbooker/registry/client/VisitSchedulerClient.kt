package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils.ClientUtils.Companion.isNotFoundError
import java.time.Duration

@Component
class VisitSchedulerClient(
  val objectMapper: ObjectMapper,
  @param:Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @param:Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getSupportedPublicPrisons(): List<String> {
    val uri = "/config/prisons/user-type/PUBLIC/supported"
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<String>>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          LOG.error("getSupportedPrisons Failed for get request $uri")
          Mono.error(e)
        } else {
          LOG.error("getSupportedPrisons NOT_FOUND for get request $uri")
          Mono.empty()
        }
      }
      .blockOptional(apiTimeout).orElse(emptyList())
  }
}
