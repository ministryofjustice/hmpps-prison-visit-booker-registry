package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.SearchBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerPrisonerAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.UpdatePrisonerPrisonValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorForPrisonerBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@Service
class BookerDetailsStoreService(
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: PermittedPrisonerRepository,
  private val visitorRepository: PermittedVisitorRepository,
  private val prisonerSearchService: PrisonerSearchService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun storeBookerPrisoner(bookerReference: String, createPermittedPrisonerDto: CreatePermittedPrisonerDto): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsStoreService storeBookerPrisoner for booker $bookerReference")

    val booker = getBooker(bookerReference)
    if (booker.permittedPrisoners.any { createPermittedPrisonerDto.prisonerId == it.prisonerId }) {
      LOG.error("Prisoner already exists for booker $bookerReference")
      throw BookerPrisonerAlreadyExistsException("BookerPrisoner for $bookerReference already exists")
    }

    val permittedPrisoner = prisonerRepository.saveAndFlush(
      PermittedPrisoner(
        bookerId = booker.id,
        booker = booker,
        prisonerId = createPermittedPrisonerDto.prisonerId,
        active = true,
        prisonCode = createPermittedPrisonerDto.prisonCode,
      ),
    )

    booker.permittedPrisoners.add(permittedPrisoner)

    LOG.info("Prisoner added to permitted prisoners for booker $bookerReference")
    return PermittedPrisonerDto(permittedPrisoner)
  }

  @Transactional
  fun clearBookerDetails(bookerReference: String): BookerDto {
    LOG.info("Enter BookerDetailsStoreService clearBookerDetails for booker $bookerReference")

    val booker = getBooker(bookerReference)
    booker.permittedPrisoners.clear()
    val bookerDto = BookerDto(bookerRepository.saveAndFlush(booker))

    return bookerDto
  }

  @Transactional(readOnly = true)
  fun getPermittedPrisoners(reference: String, active: Boolean?): List<PermittedPrisonerDto> {
    LOG.info("Enter BookerDetailsStoreService getPermittedPrisoners for booker $reference")

    val bookerByReference = getBooker(reference)
    val associatedPrisoners =
      active?.let {
        prisonerRepository.findByBookerIdAndActive(bookerByReference.id, it)
      } ?: prisonerRepository.findByBookerId(bookerByReference.id)

    return associatedPrisoners.map(::PermittedPrisonerDto)
  }

  @Transactional(readOnly = true)
  fun getPermittedVisitors(bookerReference: String, prisonerId: String, active: Boolean?): List<PermittedVisitorDto> {
    LOG.info("Enter BookerDetailsStoreService getPermittedVisitors for booker $bookerReference")

    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    val visitors = active?.let {
      visitorRepository.findByPermittedPrisonerIdAndActive(prisoner.id, active)
    } ?: visitorRepository.findByPermittedPrisonerId(prisoner.id)

    return visitors.map { PermittedVisitorDto(it) }
  }

  @Transactional
  fun storeBookerPrisonerVisitor(bookerReference: String, prisonerId: String, createPermittedVisitorDto: CreatePermittedVisitorDto): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsStoreService storeBookerPrisonerVisitor for booker $bookerReference, prisoner $prisonerId, visitor ${createPermittedVisitorDto.visitorId}")

    val bookerPrisoner = getPermittedPrisoner(bookerReference, prisonerId)

    if (bookerPrisoner.permittedVisitors.any { createPermittedVisitorDto.visitorId == it.visitorId }) {
      LOG.warn("Visitor  ${createPermittedVisitorDto.visitorId} already exists for booker $bookerReference prisoner $prisonerId")
      return PermittedVisitorDto(
        visitorId = createPermittedVisitorDto.visitorId,
        active = bookerPrisoner.permittedVisitors.first { it.visitorId == createPermittedVisitorDto.visitorId }.active,
      )
    }

    val permittedVisitor = visitorRepository.saveAndFlush(
      PermittedVisitor(
        permittedPrisonerId = bookerPrisoner.id,
        permittedPrisoner = bookerPrisoner,
        visitorId = createPermittedVisitorDto.visitorId,
        active = true,
      ),
    )
    bookerPrisoner.permittedVisitors.add(permittedVisitor)

    LOG.info("Visitor ${createPermittedVisitorDto.visitorId} added to permitted visitors for booker $bookerReference prisoner $prisonerId")

    return PermittedVisitorDto(permittedVisitor)
  }

  @Transactional
  fun activateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsStoreService activateBookerPrisoner for booker $bookerReference")
    val permittedPrisonerDto = setPrisonerBooker(bookerReference, prisonerId, true)
    return permittedPrisonerDto
  }

  @Transactional
  fun deactivateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsStoreService deactivateBookerPrisoner for booker $bookerReference")
    val permittedPrisonerDto = setPrisonerBooker(bookerReference, prisonerId, false)
    return permittedPrisonerDto
  }

  @Transactional
  fun activateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsStoreService activateBookerPrisonerVisitor for booker $bookerReference")
    val permittedVisitorDto = setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, true)
    return permittedVisitorDto
  }

  @Transactional
  fun deactivateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsStoreService deactivateBookerPrisonerVisitor for booker $bookerReference")
    val permittedVisitorDto = setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, false)
    return permittedVisitorDto
  }

  @Transactional
  fun unlinkBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long) {
    LOG.info("Enter BookerDetailsStoreService unlinkBookerPrisonerVisitor for booker $bookerReference, unlink visitor $visitorId")

    val result = visitorRepository.deleteVisitorBy(bookerReference, prisonerId, visitorId)
    if (result != 1) {
      LOG.error("Failed to unlink visitor for booker $bookerReference, prisoner $prisonerId, visitor $visitorId")
      throw VisitorForPrisonerBookerNotFoundException("Failed to unlink visitor for booker $bookerReference, prisoner $prisonerId, visitor $visitorId")
    }
  }

  @Transactional
  fun updateBookerPrisonerPrison(bookerReference: String, prisonerId: String, newPrisonCode: String): PermittedPrisonerDto {
    LOG.info("Enter updateBookerPrisonerPrison for booker $bookerReference to update prisoner $prisonerId's prison code to $newPrisonCode")

    val booker = getBooker(bookerReference)

    // if validation passes, update prisoner's prison
    validateUpdateBookerPrisonerPrison(booker, prisonerId, newPrisonCode)

    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    val oldPrisonCode = prisoner.prisonCode
    prisoner.prisonCode = newPrisonCode

    LOG.info("Prisoner - $prisonerId's prison code updated from $oldPrisonCode to $newPrisonCode for booker $bookerReference")
    return PermittedPrisonerDto(prisoner)
  }

  @Transactional(readOnly = true)
  fun searchForBooker(searchCriteria: SearchBookerDto): List<BookerDto> = findBookersByEmail(searchCriteria.email).map { BookerDto(it) }.toList()

  @Transactional(readOnly = true)
  fun getBookerByReference(bookerReference: String): BookerDto = BookerDto(getBooker(bookerReference))

  @Transactional(readOnly = true)
  fun getBooker(bookerReference: String): Booker = bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker for reference : $bookerReference not found")

  @Transactional(readOnly = true)
  fun getPermittedPrisoner(bookerReference: String, prisonerId: String): PermittedPrisoner = prisonerRepository.findByBookerIdAndPrisonerId(bookerReference, prisonerId) ?: throw PrisonerNotFoundException("Permitted prisoner for - $bookerReference/$prisonerId not found")

  private fun setPrisonerBooker(
    bookerReference: String,
    prisonerId: String,
    active: Boolean,
  ): PermittedPrisonerDto {
    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    prisoner.active = active
    return PermittedPrisonerDto(prisoner)
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

  private fun findVisitorBy(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitor = visitorRepository.findVisitorBy(bookerReference, prisonerId, visitorId) ?: throw VisitorForPrisonerBookerNotFoundException("Visitor for prisoner booker $bookerReference/$prisonerId/$visitorId not found")

  private fun validateUpdateBookerPrisonerPrison(booker: Booker, prisonerId: String, newPrisonCode: String) {
    var errorMessage: String

    // if prisoner not found for booker, throw an exception
    if (booker.permittedPrisoners.none { prisonerId == it.prisonerId }) {
      errorMessage = "Prisoner $prisonerId has not been added for booker ${booker.reference}"
      LOG.error(errorMessage)
      throw UpdatePrisonerPrisonValidationException(errorMessage)
    }

    // if the prisoner isn't in the prison supplied - throw an exception
    val prisonerDetails = prisonerSearchService.getPrisoner(prisonerId)

    if (prisonerDetails.prisonId != newPrisonCode) {
      errorMessage = "Prisoner - $prisonerId is in ${prisonerDetails.prisonId} - so cannot be updated to $newPrisonCode"
      LOG.error(errorMessage)
      throw UpdatePrisonerPrisonValidationException(errorMessage)
    }
  }

  private fun findBookersByEmail(emailAddress: String): List<Booker> {
    val foundBookers = bookerRepository.findAllByEmailIgnoreCase(emailAddress)
    if (foundBookers.isNullOrEmpty()) {
      throw BookerNotFoundException("Booker(s) for email : $emailAddress not found")
    }

    return foundBookers
  }
}
