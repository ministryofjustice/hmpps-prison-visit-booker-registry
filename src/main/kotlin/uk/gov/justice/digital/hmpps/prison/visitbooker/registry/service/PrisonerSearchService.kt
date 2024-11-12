package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto

@Service
class PrisonerSearchService(
  private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(prisonerNumber: String): PrisonerDto {
    LOG.trace("Getting prisoner details for prisoner number - {}", prisonerNumber)
    val prisoner = prisonerOffenderSearchClient.getPrisonerById(prisonerNumber)
    LOG.trace("Returning prisoner details - {} for prisoner number - {}", prisoner, prisonerNumber)
    return prisoner
  }
}
