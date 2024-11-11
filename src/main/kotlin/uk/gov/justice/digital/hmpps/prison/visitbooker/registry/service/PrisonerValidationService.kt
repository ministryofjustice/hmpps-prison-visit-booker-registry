package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationErrorCodes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationErrorCodes.PRISONER_RELEASED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationErrorCodes.PRISONER_TRANSFERRED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerValidationException

@Service
class PrisonerValidationService(
  private val bookerDetailsService: BookerDetailsService,
  private val prisonerSearchService: PrisonerSearchService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun validatePrisoner(bookerReference: String, prisonerId: String) {
    LOG.info("Validate booking by booker - {} for prisoner - {}", bookerReference, prisonerId)

    val permittedPrisoner = bookerDetailsService.getPermittedPrisoner(bookerReference, prisonerId).let { permittedPrisoner ->
      PermittedPrisonerDto(
        prisonerId = permittedPrisoner.prisonerId,
        active = permittedPrisoner.active,
        prisonCode = permittedPrisoner.prisonCode,
        permittedVisitors = emptyList(),
      )
    }

    val prisonerSearchPrisoner = prisonerSearchService.getPrisoner(prisonerId)

    // validate the prisoner returned
    val errorCodes = getPrisonerValidationErrorCodes(bookerReference, permittedPrisoner, prisonerSearchPrisoner)
    if (errorCodes.isNotEmpty()) {
      LOG.info("Prisoner validation for bookerReference - {}, prisoner id - {} failed with error codes - {}", bookerReference, prisonerId, errorCodes)
      throw PrisonerValidationException(errorCodes)
    }
  }

  private fun getPrisonerValidationErrorCodes(bookerReference: String, permittedPrisoner: PermittedPrisonerDto, prisonerSearchPrisoner: PrisonerDto): List<PrisonerValidationErrorCodes> {
    val errorCodes = mutableListOf<PrisonerValidationErrorCodes>()

    if (permittedPrisoner.prisonCode != prisonerSearchPrisoner.prisonId) {
      LOG.info("Prison code {} on booker registry for prisoner - {} and booker reference - {} does not match with prison code - {} on prisoner offender search", permittedPrisoner.prisonCode, permittedPrisoner.prisonerId, bookerReference, prisonerSearchPrisoner.prisonId)
      if (hasPrisonerBeenReleased(prisonerSearchPrisoner)) {
        errorCodes.add(PRISONER_RELEASED)
      } else {
        errorCodes.add(PRISONER_TRANSFERRED)
      }
    }

    return errorCodes.toList()
  }

  private fun hasPrisonerBeenReleased(prisonerSearchPrisoner: PrisonerDto): Boolean {
    return prisonerSearchPrisoner.inOutStatus?.let { inOutStatus ->
      (inOutStatus == "OUT")
    } ?: false
  }
}
