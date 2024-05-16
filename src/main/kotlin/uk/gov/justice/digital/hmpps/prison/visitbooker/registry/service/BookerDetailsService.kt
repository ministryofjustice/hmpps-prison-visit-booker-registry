package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Prisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Visitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerVisitorRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@Service
class BookerDetailsService(
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: BookerPrisonerRepository,
  private val visitorRepository: BookerPrisonerVisitorRepository,
) {

  @Transactional
  fun getBooker(reference: String): Booker {
    return bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
  }

  @Transactional
  fun createOrUpdateBookerDetails(createBookerDto: CreateBookerDto): BookerDto {
    val booker = bookerRepository.findByEmail(createBookerDto.email)?.let {
      // clear child objects from booker
      it.prisoners.clear()
      bookerRepository.saveAndFlush(it)
    } ?: run {
      bookerRepository.saveAndFlush(Booker(email = createBookerDto.email))
    }

    val saveBooker = createChildObjects(createBookerDto, booker)
    return BookerDto(saveBooker)
  }

  @Transactional
  fun clearBookerDetails(bookerReference: String): BookerDto {
    val booker = getBooker(bookerReference)
    booker.prisoners.clear()
    return BookerDto(bookerRepository.saveAndFlush(booker))
  }

  private fun createChildObjects(
    createBookerDto: CreateBookerDto,
    booker: Booker,
  ): Booker {
    createBookerDto.prisoners.forEach { prisonerDto ->
      val prisoner = prisonerRepository.saveAndFlush(
        Prisoner(
          bookerId = booker.id,
          booker = booker,
          prisonerId = prisonerDto.prisonerId,
          active = true,
        ),
      )
      prisonerDto.visitorIds.forEach {
        val visitor = visitorRepository.saveAndFlush(
          Visitor(
            prisonerId = prisoner.id,
            prisoner = prisoner,
            visitorId = it,
            active = true,
          ),
        )
        prisoner.visitors.add(visitor)
      }
      booker.prisoners.add(prisoner)
    }

    return bookerRepository.saveAndFlush(booker)
  }
}
