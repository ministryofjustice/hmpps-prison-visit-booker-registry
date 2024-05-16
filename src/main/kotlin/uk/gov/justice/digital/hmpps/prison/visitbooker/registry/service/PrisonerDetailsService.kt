package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Prisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerRepository

@Service
class PrisonerDetailsService(
  private val bookerService: BookerDetailsService,
  private val prisonerRepository: BookerPrisonerRepository,
) {

  @Transactional(readOnly = true)
  fun getAssociatedPrisoners(reference: String, active: Boolean?): List<PrisonerDto> {
    val bookerByReference = bookerService.getBooker(reference)
    val associatedPrisoners =
      active?.let {
        prisonerRepository.findByBookerIdAndActive(bookerByReference.id, it)
      } ?: prisonerRepository.findByBookerId(bookerByReference.id)
    return associatedPrisoners.map(::PrisonerDto)
  }

  @Transactional(readOnly = true)
  fun getAssociatedPrisoner(reference: String, prisonerId: String): Prisoner? {
    val bookerByReference = bookerService.getBooker(reference)
    return prisonerRepository.findByBookerIdAndPrisonerId(bookerByReference.id, prisonerId)
  }
}
