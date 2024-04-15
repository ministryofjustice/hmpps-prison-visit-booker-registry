package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AssociatedPrisonersVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerVisitorRepository

@Service
class VisitorDetailsService(
  private val prisonersService: PrisonerDetailsService,
  private val bookerPrisonerVisitorRepository: BookerPrisonerVisitorRepository,
  private val orchestrationServiceClient: OrchestrationServiceClient,
) {
  companion object {
    private const val NOT_KNOWN = "NOT_KNOWN"
  }

  fun getAssociatedVisitors(bookerReference: String, prisonerNumber: String): List<AssociatedPrisonersVisitorDto> {
    val prisoner = getPrisoner(bookerReference, prisonerNumber)
    val associatedPrisonersVisitors = mutableListOf<AssociatedPrisonersVisitorDto>()
    val visitors = bookerPrisonerVisitorRepository.findByBookerPrisonerId(prisoner.id)
    if (visitors.isNotEmpty()) {
      val visitorsBasicContactDetailsMap = orchestrationServiceClient.getVisitorDetails(prisonerNumber, visitors.map { it.visitorId }.toList())?.associateBy { it.personId } ?: emptyMap()
      visitors.forEach {
        val visitorsBasicContactDetails = visitorsBasicContactDetailsMap[it.visitorId]
        associatedPrisonersVisitors.add(
          AssociatedPrisonersVisitorDto(
            it.visitorId,
            visitorsBasicContactDetails?.firstName ?: NOT_KNOWN,
            visitorsBasicContactDetails?.middleName,
            visitorsBasicContactDetails?.lastName ?: NOT_KNOWN,
            it.active,
          ),
        )
      }
    }

    return associatedPrisonersVisitors
  }

  private fun getPrisoner(bookerReference: String, prisonerId: String): BookerPrisoner {
    return prisonersService.getAssociatedPrisoner(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("Prisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
  }
}
