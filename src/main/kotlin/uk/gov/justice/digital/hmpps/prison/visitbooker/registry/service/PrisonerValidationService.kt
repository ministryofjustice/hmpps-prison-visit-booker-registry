package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError.PRISONER_RELEASED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError.PRISONER_TRANSFERRED_SUPPORTED_PRISON
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError.PRISONER_TRANSFERRED_UNSUPPORTED_PRISON
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.RegisterPrisonerValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils.RegisterPrisonerValidator

@Service
class PrisonerValidationService(
  private val bookerDetailsStoreService: BookerDetailsStoreService,
  private val prisonerSearchService: PrisonerSearchService,
  private val visitSchedulerService: VisitSchedulerService,
  private val registerPrisonerValidator: RegisterPrisonerValidator,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun validatePrisonerBeforeBooking(bookerReference: String, prisonerId: String) {
    LOG.info("Validate booking by booker - {} for prisoner - {}", bookerReference, prisonerId)

    val permittedPrisoner = bookerDetailsStoreService.getPermittedPrisoner(bookerReference, prisonerId).let { permittedPrisoner ->
      PermittedPrisonerDto(
        prisonerId = permittedPrisoner.prisonerId,
        active = permittedPrisoner.active,
        prisonCode = permittedPrisoner.prisonCode,
        permittedVisitors = emptyList(),
      )
    }

    val prisonerSearchPrisoner = prisonerSearchService.getPrisoner(prisonerId)

    // validate the prisoner returned
    val errorCode = getPrisonerValidationErrorCodes(bookerReference, permittedPrisoner, prisonerSearchPrisoner)
    if (errorCode != null) {
      LOG.info("Prisoner validation for bookerReference - {}, prisoner id - {} failed with error code - {}", bookerReference, prisonerId, errorCode)
      throw PrisonerValidationException(errorCode)
    }
  }

  private fun getPrisonerValidationErrorCodes(bookerReference: String, permittedPrisoner: PermittedPrisonerDto, prisonerSearchPrisoner: PrisonerDto): PrisonerValidationError? = if (permittedPrisoner.prisonCode != prisonerSearchPrisoner.prisonId) {
    LOG.info("Prison code {} on booker registry for prisoner - {} and booker reference - {} does not match with prison code - {} on prisoner offender search", permittedPrisoner.prisonCode, permittedPrisoner.prisonerId, bookerReference, prisonerSearchPrisoner.prisonId)
    if (hasPrisonerBeenReleased(prisonerSearchPrisoner)) {
      PRISONER_RELEASED
    } else {
      if (hasPrisonerMovedToSupportedPrison(prisonerSearchPrisoner)) {
        PRISONER_TRANSFERRED_SUPPORTED_PRISON
      } else {
        PRISONER_TRANSFERRED_UNSUPPORTED_PRISON
      }
    }
  } else {
    null
  }

  private fun hasPrisonerBeenReleased(prisonerSearchPrisoner: PrisonerDto): Boolean = prisonerSearchPrisoner.inOutStatus?.let { inOutStatus ->
    (inOutStatus == "OUT")
  } ?: false

  private fun hasPrisonerMovedToSupportedPrison(prisonerSearchPrisoner: PrisonerDto): Boolean = visitSchedulerService.getSupportedPublicPrisons().contains(prisonerSearchPrisoner.prisonId)

  private fun getPrisoner(prisonerId: String): PrisonerDto? {
    val prisoner = try {
      prisonerSearchService.getPrisoner(prisonerId)
    } catch (e: PrisonerNotFoundException) {
      LOG.info("Prisoner with id  $prisonerId not found on prisoner-search")
      null
    }

    return prisoner
  }

  @Throws(PrisonerValidationException::class)
  fun validatePrisoner(booker: BookerDto, registerPrisonerRequestDto: RegisterPrisonerRequestDto) {
    val existingRegisteredPrisoners = booker.permittedPrisoners

    // first validate against existing registered prisoners
    val errors = registerPrisonerValidator.validateAgainstRegisteredPrisoners(registerPrisonerRequestDto, existingRegisteredPrisoners).toMutableList()

    // if no errors check data against prisoner search
    if (errors.isEmpty()) {
      val prisoner = getPrisoner(registerPrisonerRequestDto.prisonerId)
      errors.addAll(registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisoner))
    }

    // if there were validation errors throw an exception
    if (errors.isNotEmpty()) {
      throw RegisterPrisonerValidationException(errors)
    }
  }
}
