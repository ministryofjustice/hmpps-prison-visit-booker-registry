package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerPrisonerAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerPrisonerVisitorAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.PrisonerForBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.VisitorForPrisonerBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.util.QuotableEncoder

@Service
class BookerDetailsService(
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: PermittedPrisonerRepository,
  private val visitorRepository: PermittedVisitorRepository,
) {

  @Transactional
  fun create(emailAddress: String): BookerDto {
    bookerRepository.findByEmail(emailAddress)?.let {
      throw BookerAlreadyExistsException("The given email address already exists")
    }

    val booker = bookerRepository.saveAndFlush(Booker(email = emailAddress))
    booker.reference = createBookerReference(booker.id)
    return BookerDto(booker)
  }

  @Transactional
  fun createBookerPrisoner(bookerReference: String, createPermittedPrisonerDto: CreatePermittedPrisonerDto): PermittedPrisonerDto {
    val booker = getBooker(bookerReference)
    if (booker.permittedPrisoners.any { createPermittedPrisonerDto.prisonerId == it.prisonerId }) {
      throw BookerPrisonerAlreadyExistsException("BookerPrisoner for $bookerReference already exists")
    }

    val permittedPrisoner = prisonerRepository.saveAndFlush(
      PermittedPrisoner(
        bookerId = booker.id,
        booker = booker,
        prisonerId = createPermittedPrisonerDto.prisonerId,
        active = createPermittedPrisonerDto.active,
      ),
    )

    booker.permittedPrisoners.add(permittedPrisoner)

    return PermittedPrisonerDto(permittedPrisoner)
  }

  @Transactional
  fun createBookerPrisonerVisitor(bookerReference: String, prisonerId: String, createPermittedVisitorDto: CreatePermittedVisitorDto): PermittedVisitorDto {
    val bookerPrisoner = getPermittedPrisoner(bookerReference, prisonerId)

    if (bookerPrisoner.permittedVisitors.any { createPermittedVisitorDto.visitorId == it.visitorId }) {
      throw BookerPrisonerVisitorAlreadyExistsException("BookerPrisonerVisitor for $bookerReference/$prisonerId already exists")
    }

    val permittedVisitor = visitorRepository.saveAndFlush(
      PermittedVisitor(
        permittedPrisonerId = bookerPrisoner.id,
        permittedPrisoner = bookerPrisoner,
        visitorId = createPermittedVisitorDto.visitorId,
        active = createPermittedVisitorDto.active,
      ),
    )
    bookerPrisoner.permittedVisitors.add(permittedVisitor)

    return PermittedVisitorDto(permittedVisitor)
  }

  fun createBookerReference(bookerId: Long): String {
    val existingReference = bookerRepository.findByBookerId(bookerId)
    if (existingReference.isNullOrBlank()) {
      return QuotableEncoder(minLength = 10).encode(bookerId)
    }
    return existingReference
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

  @Transactional
  fun activateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    return setPrisonerBooker(bookerReference, prisonerId, true)
  }

  @Transactional
  fun deactivateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    return setPrisonerBooker(bookerReference, prisonerId, false)
  }

  @Transactional
  fun activateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    return setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, true)
  }

  @Transactional
  fun deactivateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    return setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, false)
  }

  private fun getBooker(bookerReference: String): Booker {
    return bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker for reference : $bookerReference not found")
  }

  private fun findBookerByEmail(emailAddress: String): Booker {
    return bookerRepository.findByEmail(emailAddress) ?: throw BookerNotFoundException("Booker for email : $emailAddress not found")
  }

  private fun getPermittedPrisoner(bookerReference: String, prisonerId: String): PermittedPrisoner {
    return prisonerRepository.findByBookerIdAndPrisonerId(bookerReference, prisonerId) ?: throw PrisonerForBookerNotFoundException("Permitted prisoner for - $bookerReference/$prisonerId not found")
  }

  private fun findVisitorBy(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitor {
    return visitorRepository.findVisitorBy(bookerReference, prisonerId, visitorId) ?: throw VisitorForPrisonerBookerNotFoundException("Visitor for prisoner booker $bookerReference/$prisonerId/$visitorId not found")
  }

  private fun setVisitorPrisonerBooker(
    bookerReference: String,
    prisonerId: String,
    visitorId: Long,
    active: Boolean,
  ): PermittedVisitorDto {
    val visitor = findVisitorBy(bookerReference, prisonerId, visitorId)
    visitor.active = active
    return PermittedVisitorDto(visitor)
  }

  private fun setPrisonerBooker(
    bookerReference: String,
    prisonerId: String,
    active: Boolean,
  ): PermittedPrisonerDto {
    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    prisoner.active = active
    return PermittedPrisonerDto(prisoner)
  }

  @Transactional(readOnly = true)
  fun getBookerByEmail(emailAddress: String): BookerDto {
    return BookerDto(findBookerByEmail(emailAddress))
  }
}
