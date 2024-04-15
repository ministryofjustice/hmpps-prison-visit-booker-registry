package uk.gov.justice.digital.hmpps.oneloginuserregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oneloginuserregistry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AssociatedPrisonersVisitorDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.orchestration.BasicContactDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.BookerPrisonerVisitorRepository

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
          AssociatedPrisonersVisitorDto(visitorsBasicContactDetails ?: getBlankBasicContactInfo(it.visitorId), it),
        )
      }
    }

    return associatedPrisonersVisitors
  }

  private fun getBlankBasicContactInfo(personId: Long): BasicContactDto {
    return BasicContactDto(personId, NOT_KNOWN, null, NOT_KNOWN)
  }

  private fun getPrisoner(bookerReference: String, prisonerId: String): BookerPrisoner {
    return prisonersService.getAssociatedPrisoner(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("Prisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
  }
}
