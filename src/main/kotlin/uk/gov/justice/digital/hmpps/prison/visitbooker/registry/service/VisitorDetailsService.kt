package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.VisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Prisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerVisitorRepository

@Service
class VisitorDetailsService(
  private val prisonersService: PrisonerDetailsService,
  private val visitorRepository: BookerPrisonerVisitorRepository,
) {
  fun getAssociatedVisitors(bookerReference: String, prisonerId: String, active: Boolean?): List<VisitorDto> {
    val prisoner = getPrisoner(bookerReference, prisonerId)
    val visitors = active?.let {
      visitorRepository.findByPrisonerIdAndActive(prisoner.id, active)
    } ?: visitorRepository.findByPrisonerId(prisoner.id)
    return visitors.map { VisitorDto(it) }
  }

  private fun getPrisoner(bookerReference: String, prisonerId: String): Prisoner {
    return prisonersService.getAssociatedPrisoner(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("Prisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
  }
}
