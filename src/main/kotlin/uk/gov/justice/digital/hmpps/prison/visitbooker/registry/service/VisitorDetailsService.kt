package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonerVisitorsDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerVisitorRepository

@Service
class VisitorDetailsService(
  private val prisonersService: PrisonerDetailsService,
  private val bookerPrisonerVisitorRepository: BookerPrisonerVisitorRepository,
) {
  fun getAssociatedVisitors(bookerReference: String, prisonerNumber: String, active: Boolean?): List<BookerPrisonerVisitorsDto> {
    val prisoner = getPrisoner(bookerReference, prisonerNumber)
    val visitors = active?.let {
      bookerPrisonerVisitorRepository.findByBookerPrisonerIdAndActive(prisoner.id, active)
    } ?: bookerPrisonerVisitorRepository.findByBookerPrisonerId(prisoner.id)
    return visitors.map { BookerPrisonerVisitorsDto(prisonerNumber, it) }
  }

  private fun getPrisoner(bookerReference: String, prisonerId: String): BookerPrisoner {
    return prisonersService.getAssociatedPrisoner(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("Prisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
  }
}
