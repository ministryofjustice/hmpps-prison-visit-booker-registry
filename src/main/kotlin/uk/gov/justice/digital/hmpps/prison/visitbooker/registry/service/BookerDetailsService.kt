package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
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

  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(emailAddress: String): BookerDto {
    LOG.info("Enter BookerDetailsService create")
    bookerRepository.findByEmailIgnoreCase(emailAddress)?.let {
      LOG.error("Found existing user for given email address - {}", emailAddress)
      throw BookerAlreadyExistsException("The given email address - $emailAddress already exists")
    }

    val booker = bookerRepository.saveAndFlush(Booker(email = emailAddress))
    booker.reference = createBookerReference(booker.id)
    LOG.info("Booker created with email address - {}, returning new booker with reference {}", emailAddress, booker.reference)
    return BookerDto(booker)
  }

  @Transactional
  fun createBookerPrisoner(bookerReference: String, createPermittedPrisonerDto: CreatePermittedPrisonerDto): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsService createBookerPrisoner for booker {}", bookerReference)

    val booker = getBooker(bookerReference)
    if (booker.permittedPrisoners.any { createPermittedPrisonerDto.prisonerId == it.prisonerId }) {
      LOG.error("Prisoner already exists for booker {}", bookerReference)
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

    LOG.info("Prisoner added to permitted prisoners for booker {}", bookerReference)
    return PermittedPrisonerDto(permittedPrisoner)
  }

  @Transactional
  fun createBookerPrisonerVisitor(bookerReference: String, prisonerId: String, createPermittedVisitorDto: CreatePermittedVisitorDto): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsService createBookerPrisonerVisitor for booker {}", bookerReference)

    val bookerPrisoner = getPermittedPrisoner(bookerReference, prisonerId)

    if (bookerPrisoner.permittedVisitors.any { createPermittedVisitorDto.visitorId == it.visitorId }) {
      LOG.error("Visitor already exists for booker {}", bookerReference)
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

    LOG.info("Visitor added to permitted visitors for booker {}", bookerReference)
    return PermittedVisitorDto(permittedVisitor)
  }

  fun createBookerReference(bookerId: Long): String {
    LOG.info("Enter BookerDetailsService createBookerReference for bookerId {}", bookerId)

    val existingReference = bookerRepository.findByBookerId(bookerId)
    if (existingReference.isNullOrBlank()) {
      return QuotableEncoder(minLength = 10).encode(bookerId)
    }
    return existingReference
  }

  @Transactional
  fun clearBookerDetails(bookerReference: String): BookerDto {
    LOG.info("Enter BookerDetailsService clearBookerDetails for booker {}", bookerReference)

    val booker = getBooker(bookerReference)
    booker.permittedPrisoners.clear()
    return BookerDto(bookerRepository.saveAndFlush(booker))
  }

  @Transactional(readOnly = true)
  fun getPermittedPrisoners(reference: String, active: Boolean?): List<PermittedPrisonerDto> {
    LOG.info("Enter BookerDetailsService getPermittedPrisoners for booker {}", reference)

    val bookerByReference = getBooker(reference)
    val associatedPrisoners =
      active?.let {
        prisonerRepository.findByBookerIdAndActive(bookerByReference.id, it)
      } ?: prisonerRepository.findByBookerId(bookerByReference.id)
    return associatedPrisoners.map(::PermittedPrisonerDto)
  }

  @Transactional(readOnly = true)
  fun getPermittedVisitors(bookerReference: String, prisonerId: String, active: Boolean?): List<PermittedVisitorDto> {
    LOG.info("Enter BookerDetailsService getPermittedVisitors for booker {}", bookerReference)

    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    val visitors = active?.let {
      visitorRepository.findByPermittedPrisonerIdAndActive(prisoner.id, active)
    } ?: visitorRepository.findByPermittedPrisonerId(prisoner.id)
    return visitors.map { PermittedVisitorDto(it) }
  }

  @Transactional
  fun activateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsService activateBookerPrisoner for booker {}", bookerReference)
    return setPrisonerBooker(bookerReference, prisonerId, true)
  }

  @Transactional
  fun deactivateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsService deactivateBookerPrisoner for booker {}", bookerReference)
    return setPrisonerBooker(bookerReference, prisonerId, false)
  }

  @Transactional
  fun activateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsService activateBookerPrisonerVisitor for booker {}", bookerReference)
    return setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, true)
  }

  @Transactional
  fun deactivateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsService deactivateBookerPrisonerVisitor for booker {}", bookerReference)
    return setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, false)
  }

  @Transactional(readOnly = true)
  fun getBookerByEmail(emailAddress: String): BookerDto {
    LOG.info("Enter BookerDetailsService getBookerByEmail")
    return BookerDto(findBookerByEmail(emailAddress))
  }

  private fun getBooker(bookerReference: String): Booker {
    return bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker for reference : $bookerReference not found")
  }

  private fun findBookerByEmail(emailAddress: String): Booker {
    return bookerRepository.findByEmailIgnoreCase(emailAddress) ?: throw BookerNotFoundException("Booker for email : $emailAddress not found")
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
}
