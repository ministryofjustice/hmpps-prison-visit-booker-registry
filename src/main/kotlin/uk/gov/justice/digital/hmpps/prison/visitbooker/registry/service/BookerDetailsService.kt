package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerAuditDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.SearchBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.RegisterPrisonerValidationException

@Service
class BookerDetailsService(
  private val bookerAuditService: BookerAuditService,
  private val prisonerValidationService: PrisonerValidationService,
  private val bookerDetailsStoreService: BookerDetailsStoreService,
  private val snsService: SnsService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun createBookerPrisoner(bookerReference: String, createPermittedPrisonerDto: CreatePermittedPrisonerDto): PermittedPrisonerDto {
    LOG.info("Enter BookerDetailsService createBookerPrisoner for booker {}", bookerReference)

    val permittedPrisoner = bookerDetailsStoreService.storeBookerPrisoner(bookerReference, createPermittedPrisonerDto)

    bookerAuditService.auditAddPrisoner(bookerReference = bookerReference, prisonNumber = createPermittedPrisonerDto.prisonerId)
    LOG.info("Prisoner added to permitted prisoners for booker {}", bookerReference)
    return permittedPrisoner
  }

  fun createBookerPrisonerVisitor(bookerReference: String, prisonerId: String, createPermittedVisitorDto: CreatePermittedVisitorDto): PermittedVisitorDto {
    LOG.info("Enter BookerDetailsService createBookerPrisonerVisitor for booker $bookerReference, prisoner $prisonerId, visitor ${createPermittedVisitorDto.visitorId}")

    val permittedVisitor = bookerDetailsStoreService.storeBookerPrisonerVisitor(bookerReference, prisonerId, createPermittedVisitorDto)

    bookerAuditService.auditAddVisitor(bookerReference = bookerReference, prisonNumber = prisonerId, visitorId = createPermittedVisitorDto.visitorId)

    if (createPermittedVisitorDto.sendNotificationFlag == true) {
      snsService.sendBookerPrisonerVisitorApprovedEvent(bookerReference, prisonerId, createPermittedVisitorDto.visitorId.toString())
    }

    return permittedVisitor
  }

  fun clearBookerDetails(bookerReference: String): BookerDto {
    LOG.info("Enter BookerDetailsService clearBookerDetails for booker {}", bookerReference)

    val bookerDto = bookerDetailsStoreService.clearBookerDetails(bookerReference)

    bookerAuditService.auditClearBookerDetails(bookerReference = bookerReference)
    return bookerDto
  }

  fun getPermittedPrisoners(reference: String): List<PermittedPrisonerDto> {
    LOG.info("Enter BookerDetailsService getPermittedPrisoners for booker $reference")

    return bookerDetailsStoreService.getPermittedPrisoners(reference)
  }

  fun getPermittedVisitors(bookerReference: String, prisonerId: String): List<PermittedVisitorDto> {
    LOG.info("Enter BookerDetailsService getPermittedVisitors for booker $bookerReference")

    return bookerDetailsStoreService.getPermittedVisitors(bookerReference, prisonerId)
  }

  fun unlinkBookerPrisonerVisitor(bookerReference: String, prisonerId: String, visitorId: Long) {
    LOG.info("Enter BookerDetailsService unlinkBookerPrisonerVisitor for booker $bookerReference, unlink visitor $visitorId")

    bookerDetailsStoreService.unlinkBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)

    bookerAuditService.auditUnlinkVisitor(bookerReference = bookerReference, prisonNumber = prisonerId, visitorId = visitorId)
  }

  fun registerPrisoner(bookerReference: String, registerPrisonerRequestDto: RegisterPrisonerRequestDto) {
    LOG.info("Register booker called with for booker $bookerReference with request details - $registerPrisonerRequestDto")
    try {
      prisonerValidationService.validatePrisoner(bookerReference, registerPrisonerRequestDto)
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

  fun searchForBooker(searchCriteria: SearchBookerDto): List<BookerDto> = bookerDetailsStoreService.searchForBooker(searchCriteria)

  fun getBookerByReference(bookerReference: String): BookerDto = bookerDetailsStoreService.getBookerByReference(bookerReference)

  fun updateBookerPrisonerPrison(bookerReference: String, prisonerId: String, newPrisonCode: String): PermittedPrisonerDto {
    LOG.info("Enter updateBookerPrisonerPrison for booker $bookerReference to update prisoner $prisonerId's prison code to $newPrisonCode")

    val prisoner = bookerDetailsStoreService.updateBookerPrisonerPrison(bookerReference, prisonerId, newPrisonCode)

    bookerAuditService.auditUpdateBookerPrisonerPrisonCode(bookerReference = bookerReference, prisonNumber = prisonerId, newPrisonCode = newPrisonCode)

    return prisoner
  }

  fun getBookerAudit(bookerReference: String): List<BookerAuditDto> {
    LOG.info("Get booker audit called for booker - $bookerReference")
    return bookerAuditService.getBookerAudit(bookerReference = bookerReference).map { BookerAuditDto(it) }
  }

  private fun auditPrisonerSearch(
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
}
