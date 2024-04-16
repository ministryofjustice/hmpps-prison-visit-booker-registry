package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration.BasicContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerVisitorRepository

@Service
class VisitorDetailsService(
  private val prisonersService: PrisonerDetailsService,
  private val bookerPrisonerVisitorRepository: BookerPrisonerVisitorRepository,
  private val orchestrationServiceClient: OrchestrationServiceClient,
) {
  fun getAssociatedVisitors(bookerReference: String, prisonerNumber: String): List<BasicContactDto> {
    val prisoner = getPrisoner(bookerReference, prisonerNumber)
    val visitors = bookerPrisonerVisitorRepository.findByBookerPrisonerIdAndActive(prisoner.id, true)
    return if (visitors.isNotEmpty()) {
      orchestrationServiceClient.getVisitorDetails(prisonerNumber, visitors.map { it.visitorId }.toList()) ?: emptyList()
    } else {
      emptyList()
    }
  }

  private fun getPrisoner(bookerReference: String, prisonerId: String): BookerPrisoner {
    return prisonersService.getAssociatedPrisoner(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("Prisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
  }
}
