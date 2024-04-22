package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonersDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@Service
class PrisonerDetailsService(
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: BookerPrisonerRepository,
) {
  fun getAssociatedPrisoners(reference: String, active: Boolean?): List<BookerPrisonersDto> {
    val bookerByReference = getBooker(reference)
    val associatedPrisoners =
      active?.let {
        prisonerRepository.findByBookerIdAndActive(bookerByReference.id, it)
      } ?: prisonerRepository.findByBookerId(bookerByReference.id)
    return associatedPrisoners.map(::BookerPrisonersDto)
  }

  fun getAssociatedPrisoner(reference: String, prisonerId: String): BookerPrisoner? {
    val bookerByReference = getBooker(reference)
    return prisonerRepository.findByBookerIdAndPrisonNumber(bookerByReference.id, prisonerId)
  }

  private fun getBooker(reference: String): Booker {
    return bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
  }
}
