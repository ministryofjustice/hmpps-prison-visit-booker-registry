package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerAuditDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.SearchBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerPrisonerAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerPrisonerVisitorAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.RegisterPrisonerValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.UpdatePrisonerPrisonValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorForPrisonerBookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@Service
class BookerDetailsService(
  private val bookerAuditService: BookerAuditService,
  private val prisonerValidationService: PrisonerValidationService,
  private val prisonerSearchService: PrisonerSearchService,
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: PermittedPrisonerRepository,
  private val visitorRepository: PermittedVisitorRepository,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
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
        prisonCode = createPermittedPrisonerDto.prisonCode,
      ),
    )

    booker.permittedPrisoners.add(permittedPrisoner)

    bookerAuditService.auditAddPrisoner(bookerReference = booker.reference, prisonNumber = createPermittedPrisonerDto.prisonerId)
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

    bookerAuditService.auditAddVisitor(bookerReference = bookerReference, prisonNumber = bookerPrisoner.prisonerId, visitorId = createPermittedVisitorDto.visitorId)
    return PermittedVisitorDto(permittedVisitor)
  }

  @Transactional
  fun clearBookerDetails(bookerReference: String): BookerDto {
    LOG.info("Enter BookerDetailsService clearBookerDetails for booker {}", bookerReference)

    val booker = getBooker(bookerReference)
    booker.permittedPrisoners.clear()
    val bookerDto = BookerDto(bookerRepository.saveAndFlush(booker))
    bookerAuditService.auditClearBookerDetails(bookerReference = bookerReference)
    return bookerDto
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
    val permittedPrisonerDto = setPrisonerBooker(bookerReference, prisonerId, true)
    bookerAuditService.auditActivatePrisoner(bookerReference = bookerReference, prisonNumber = prisonerId)
    return permittedPrisonerDto
  }

  @Transactional
  fun deactivateBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsService deactivateBookerPrisoner for booker {}", bookerReference)
    val permittedPrisonerDto = setPrisonerBooker(bookerReference, prisonerId, false)
    bookerAuditService.auditDeactivatePrisoner(bookerReference = bookerReference, prisonNumber = prisonerId)
    return permittedPrisonerDto
  }

  @Transactional
  fun activateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsService activateBookerPrisonerVisitor for booker {}", bookerReference)
    val permittedVisitorDto = setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, true)
    bookerAuditService.auditActivateVisitor(bookerReference = bookerReference, prisonNumber = prisonerId, visitorId = visitorId)
    return permittedVisitorDto
  }

  @Transactional
  fun deactivateBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsService deactivateBookerPrisonerVisitor for booker {}", bookerReference)
    val permittedVisitorDto = setVisitorPrisonerBooker(bookerReference, prisonerId, visitorId, false)
    bookerAuditService.auditDeactivateVisitor(bookerReference = bookerReference, prisonNumber = prisonerId, visitorId = visitorId)
    return permittedVisitorDto
  }

  @Transactional
  fun unlinkBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long) {
    LOG.info("Enter BookerDetailsService unlinkBookerPrisonerVisitor for booker $bookerReference, unlink visitor $visitorId")

    visitorRepository.deleteVisitorBy(bookerReference, prisonerId, visitorId)
    bookerAuditService.auditUnlinkVisitor(bookerReference = bookerReference, prisonNumber = prisonerId, visitorId = visitorId)
  }

  @Transactional(readOnly = true)
  fun searchForBooker(searchCriteria: SearchBookerDto): List<BookerDto> = findBookersByEmail(searchCriteria.email).map { BookerDto(it) }.toList()

  @Transactional(readOnly = true)
  fun getBookerByReference(bookerReference: String): BookerDto = BookerDto(getBooker(bookerReference))

  @Transactional
  fun registerPrisoner(bookerReference: String, registerPrisonerRequestDto: RegisterPrisonerRequestDto) {
    LOG.info("Register booker called with for booker -  {} with request details - {} ", bookerReference, registerPrisonerRequestDto)
    val booker = BookerDto(getBooker(bookerReference))
    try {
      prisonerValidationService.validatePrisoner(booker, registerPrisonerRequestDto)
      auditPrisonerSearch(bookerReference, registerPrisonerRequestDto, true)
    } catch (e: RegisterPrisonerValidationException) {
      LOG.info("Validation failed for register prisoner with booker reference -  {} and request details - {} with error(s) [{}]", bookerReference, registerPrisonerRequestDto, e.errors)
      auditPrisonerSearch(bookerReference, registerPrisonerRequestDto, false, e.errors)
      throw e
    }

    // register prisoner against booker
    val createPermittedPrisoner = CreatePermittedPrisonerDto(registerPrisonerRequestDto)
    createBookerPrisoner(bookerReference, createPermittedPrisoner)
  }

  @Transactional(readOnly = true)
  fun getBookerAudit(bookerReference: String): List<BookerAuditDto> {
    LOG.info("Get booker audit called for booker - {}", bookerReference)
    val booker = getBooker(bookerReference)
    return bookerAuditService.getBookerAudit(bookerReference = booker.reference).map { BookerAuditDto(it) }
  }

  fun getPermittedPrisoner(bookerReference: String, prisonerId: String): PermittedPrisoner = prisonerRepository.findByBookerIdAndPrisonerId(bookerReference, prisonerId) ?: throw PrisonerNotFoundException("Permitted prisoner for - $bookerReference/$prisonerId not found")

  @Transactional
  fun auditPrisonerSearch(
    bookerReference: String,
    registerPrisonerRequestDto: RegisterPrisonerRequestDto,
    success: Boolean,
    validationFailures: List<RegisterPrisonerValidationError>? = null,
  ) {
    bookerAuditService.auditPrisonerSearchDetails(
      bookerReference = bookerReference,
      prisonerNumber = registerPrisonerRequestDto.prisonerId,
      firstName = registerPrisonerRequestDto.prisonerFirstName,
      lastName = registerPrisonerRequestDto.prisonerLastName,
      dob = registerPrisonerRequestDto.prisonerDateOfBirth,
      prisonCode = registerPrisonerRequestDto.prisonCode,
      success = success,
      failures = validationFailures,
    )
  }

  @Transactional
  fun updateBookerPrisonerPrison(bookerReference: String, prisonerId: String, newPrisonCode: String): PermittedPrisonerDto {
    LOG.info("Enter updateBookerPrisonerPrison for booker {} to update prisoner {}'s prison code to {}", bookerReference, prisonerId, newPrisonCode)

    val booker = getBooker(bookerReference)

    // if validation passes, update prisoner's prison
    validateUpdateBookerPrisonerPrison(booker, prisonerId, newPrisonCode)

    val prisoner = getPermittedPrisoner(bookerReference, prisonerId)
    val oldPrisonCode = prisoner.prisonCode
    prisoner.prisonCode = newPrisonCode

    bookerAuditService.auditUpdateBookerPrisonerPrisonCode(bookerReference = booker.reference, prisonNumber = prisonerId, oldPrisonCode = oldPrisonCode, newPrisonCode = newPrisonCode)
    LOG.info("Prisoner - {}'s prison code updated from {} to {} for booker {}", prisonerId, prisoner.prisonerId, newPrisonCode, bookerReference)
    return PermittedPrisonerDto(prisoner)
  }

  private fun getBooker(bookerReference: String): Booker = bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker for reference : $bookerReference not found")

  private fun findBookersByEmail(emailAddress: String): List<Booker> {
    val foundBookers = bookerRepository.findAllByEmailIgnoreCase(emailAddress)
    if (foundBookers.isNullOrEmpty()) {
      throw BookerNotFoundException("Booker(s) for email : $emailAddress not found")
    }

    return foundBookers
  }

  private fun findVisitorBy(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitor = visitorRepository.findVisitorBy(bookerReference, prisonerId, visitorId) ?: throw VisitorForPrisonerBookerNotFoundException("Visitor for prisoner booker $bookerReference/$prisonerId/$visitorId not found")

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

  @Throws
  private fun validateUpdateBookerPrisonerPrison(booker: Booker, prisonerId: String, newPrisonCode: String) {
    var errorMessage: String
    // if prisoner not found for booker throw an exception
    if (booker.permittedPrisoners.none { prisonerId == it.prisonerId }) {
      errorMessage = "Prisoner $prisonerId has not been added for booker ${booker.reference}"
      LOG.error(errorMessage)
      throw UpdatePrisonerPrisonValidationException(errorMessage)
    }

    // if prisoner is not in the prison supplied - throw an exception
    val prisonerDetails = prisonerSearchService.getPrisoner(prisonerId)

    if (prisonerDetails.prisonId != newPrisonCode) {
      errorMessage = "Prisoner - $prisonerId is in ${prisonerDetails.prisonId} - so cannot be updated to $newPrisonCode"
      LOG.error(errorMessage)
      throw UpdatePrisonerPrisonValidationException(errorMessage)
    }
  }
}
