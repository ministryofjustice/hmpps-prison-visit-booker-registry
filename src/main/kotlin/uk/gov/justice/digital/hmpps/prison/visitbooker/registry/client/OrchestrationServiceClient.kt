package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration.BasicContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration.PrisonerBasicInfoDto
import java.time.Duration

@Component
class OrchestrationServiceClient(

  @Qualifier("orchestrationServiceWebClient") private val webClient: WebClient,
  @Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {

  companion object {
    private const val ORCHESTRATION_PRISONER_DETAILS_PATH: String = "/prisoner/{prisonerIds}/basic-details"
    const val ORCHESTRATION_VISITOR_DETAILS_PATH: String = "prisoner/{prisonerId}/visitors/{visitorIds}/basic-details"
  }

  fun getPrisonerDetails(prisonerIds: List<String>): List<PrisonerBasicInfoDto>? {
    return webClient.get()
      .uri(ORCHESTRATION_PRISONER_DETAILS_PATH.replace("{prisonerIds}", prisonerIds.joinToString(",")))
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<PrisonerBasicInfoDto>>().onErrorResume { e ->
        if (e is WebClientResponseException) {
          return@onErrorResume Mono.just(emptyList<PrisonerBasicInfoDto>())
        }
        Mono.error(e)
      }.block(apiTimeout)
  }

  fun getVisitorDetails(prisonerId: String, visitorIds: List<Long>): List<BasicContactDto>? {
    return webClient.get()
      .uri(
        ORCHESTRATION_VISITOR_DETAILS_PATH.replace("{prisonerId}", prisonerId)
          .replace("{visitorIds}", visitorIds.joinToString(",")),
      )
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<List<BasicContactDto>>().onErrorResume { e ->
        if (e is WebClientResponseException) {
          return@onErrorResume Mono.just(emptyList<BasicContactDto>())
        }
        Mono.error(e)
      }.block(apiTimeout)
  }
}
