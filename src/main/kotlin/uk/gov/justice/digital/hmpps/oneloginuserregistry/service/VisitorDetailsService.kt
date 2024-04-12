package uk.gov.justice.digital.hmpps.oneloginuserregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oneloginuserregistry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AssociatedPrisonersVisitorDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.orchestration.BasicContactDto
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

  fun getAssociatedVisitors(reference: String, prisonerId: String): List<AssociatedPrisonersVisitorDto> {
    val prisoner = prisonersService.getAssociatedPrisoner(reference, prisonerId)
    val associatedPrisonersVisitors = mutableListOf<AssociatedPrisonersVisitorDto>()
    return prisoner?.let { associatedPrisoner ->
      val visitors = bookerPrisonerVisitorRepository.findByBookerPrisonerId(associatedPrisoner.id)
      if (visitors.isNotEmpty()) {
        val visitorsBasicContactDetailsMap = orchestrationServiceClient.getVisitorDetails(prisonerId, visitors.map { it.visitorId }.toList())?.associateBy { it.personId } ?: emptyMap()
        visitors.forEach {
          val visitorsBasicContactDetails = visitorsBasicContactDetailsMap[it.visitorId]
          associatedPrisonersVisitors.add(
            AssociatedPrisonersVisitorDto(visitorsBasicContactDetails ?: getBlankBasicContactInfo(it.visitorId), it),
          )
        }
      }
      associatedPrisonersVisitors
    } ?: emptyList()
  }

  private fun getBlankBasicContactInfo(personId: Long): BasicContactDto {
    return BasicContactDto(personId, NOT_KNOWN, null, NOT_KNOWN)
  }
}