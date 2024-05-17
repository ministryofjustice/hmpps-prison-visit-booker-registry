package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@Service
class BookerDetailsService(
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: PermittedPrisonerRepository,
  private val visitorRepository: PermittedVisitorRepository,
) {

  @Transactional
  fun createOrUpdateBookerDetails(createBookerDto: CreateBookerDto): BookerDto {
    val booker = bookerRepository.findByEmail(createBookerDto.email)?.let {
      if (it.permittedPrisoners.isNotEmpty()) {
        // clear child objects from booker
        it.permittedPrisoners.clear()
      }
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
    booker.permittedPrisoners.clear()
    return BookerDto(bookerRepository.saveAndFlush(booker))
  }

  @Transactional(readOnly = true)
  fun getAssociatedPrisoners(reference: String, active: Boolean?): List<PermittedPrisonerDto> {
    val bookerByReference = getBooker(reference)
    val associatedPrisoners =
      active?.let {
        prisonerRepository.findByBookerIdAndActive(bookerByReference.id, it)
      } ?: prisonerRepository.findByBookerId(bookerByReference.id)
    return associatedPrisoners.map(::PermittedPrisonerDto)
  }

  fun getAssociatedPrisoner(reference: String, prisonerId: String): PermittedPrisoner? {
    val bookerByReference = getBooker(reference)
    return prisonerRepository.findByBookerIdAndPrisonerId(bookerByReference.id, prisonerId)
  }

  @Transactional(readOnly = true)
  fun getAssociatedVisitors(bookerReference: String, prisonerId: String, active: Boolean?): List<PermittedVisitorDto> {
    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    val visitors = active?.let {
      visitorRepository.findByPermittedPrisonerIdAndActive(prisoner.id, active)
    } ?: visitorRepository.findByPermittedPrisonerId(prisoner.id)
    return visitors.map { PermittedVisitorDto(it) }
  }

  private fun getBooker(reference: String): Booker {
    return bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
  }

  private fun getPermittedPrisoner(bookerReference: String, prisonerId: String): PermittedPrisoner {
    return getAssociatedPrisoner(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("PermittedPrisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
  }

  private fun createChildObjects(
    createBookerDto: CreateBookerDto,
    booker: Booker,
  ): Booker {
    createBookerDto.permittedPrisoners.forEach { prisonerDto ->
      val permittedPrisoner = prisonerRepository.saveAndFlush(
        PermittedPrisoner(
          bookerId = booker.id,
          booker = booker,
          prisonerId = prisonerDto.prisonerId,
          active = true,
        ),
      )
      prisonerDto.visitorIds.forEach {
        val permittedVisitor = visitorRepository.saveAndFlush(
          PermittedVisitor(
            permittedPrisonerId = permittedPrisoner.id,
            permittedPrisoner = permittedPrisoner,
            visitorId = it,
            active = true,
          ),
        )
        permittedPrisoner.permittedVisitors.add(permittedVisitor)
      }
      booker.permittedPrisoners.add(permittedPrisoner)
    }

    return bookerRepository.saveAndFlush(booker)
  }
}
