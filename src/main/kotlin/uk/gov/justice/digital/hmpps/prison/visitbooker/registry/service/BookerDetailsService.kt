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
  fun getPermittedPrisoners(reference: String, active: Boolean?): List<PermittedPrisonerDto> {
    val bookerByReference = getBooker(reference)
    val associatedPrisoners =
      active?.let {
        prisonerRepository.findByBookerIdAndActive(bookerByReference.id, it)
      } ?: prisonerRepository.findByBookerId(bookerByReference.id)
    return associatedPrisoners.map(::PermittedPrisonerDto)
  }

  @Transactional(readOnly = true)
  fun getPermittedVisitors(bookerReference: String, prisonerId: String, active: Boolean?): List<PermittedVisitorDto> {
    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    val visitors = active?.let {
      visitorRepository.findByPermittedPrisonerIdAndActive(prisoner.id, active)
    } ?: visitorRepository.findByPermittedPrisonerId(prisoner.id)
    return visitors.map { PermittedVisitorDto(it) }
  }

  private fun getBooker(bookerReference: String): Booker {
    return bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker for reference : $bookerReference not found")
  }

  private fun getPermittedPrisoner(bookerReference: String, prisonerId: String): PermittedPrisoner {
    val bookerByReference = getBooker(bookerReference)
    return prisonerRepository.findByBookerIdAndPrisonerId(bookerByReference.id, prisonerId) ?: throw PrisonerForBookerNotFoundException("Permitted prisoner with prisonNumber - $prisonerId not found for booker reference - $bookerReference")
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

  @Transactional
  fun activateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    return activateDeactivatePrisoner(bookerReference, prisonerId, true)
  }

  @Transactional
  fun deactivateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    return activateDeactivatePrisoner(bookerReference, prisonerId, false)
  }

  private fun activateDeactivatePrisoner(
    bookerReference: String,
    prisonerId: String,
    active: Boolean,
  ): PermittedPrisonerDto {
    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    prisoner.active = active
    return PermittedPrisonerDto(prisoner)
  }
}
