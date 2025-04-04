package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils

import org.apache.commons.lang3.RegExUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import java.text.Normalizer
import java.time.LocalDate

@Service
class RegisterPrisonerValidator {
  fun validateAgainstRegisteredPrisoners(registerPrisonerRequest: RegisterPrisonerRequestDto, registeredPrisoners: List<PermittedPrisonerDto>): List<RegisterPrisonerValidationError> {
    val errors = mutableListOf<RegisterPrisonerValidationError>()
    val prisonerIdToRegister = registerPrisonerRequest.prisonerId

    if (registeredPrisoners.isNotEmpty()) {
      // check if the prisoner is already registered - active / inactive
      if (isPrisonerAlreadyAdded(registeredPrisoners, prisonerIdToRegister)) {
        errors.add(RegisterPrisonerValidationError.PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER)
      } else {
        // check if there are already any active prisoners for the booker
        // TODO - to be removed when we allow multiple prisoners.
        if (hasActivePrisoners(registeredPrisoners)) {
          errors.add(RegisterPrisonerValidationError.BOOKER_ALREADY_HAS_A_PRISONER)
        }
      }
    }
    return errors
  }

  fun validateAgainstPrisonerSearch(registerPrisonerRequest: RegisterPrisonerRequestDto, prisonerSearchPrisoner: PrisonerDto?): List<RegisterPrisonerValidationError> {
    val errors = mutableListOf<RegisterPrisonerValidationError>()
    if (prisonerSearchPrisoner == null) {
      errors.add(RegisterPrisonerValidationError.PRISONER_NOT_FOUND)
    } else {
      validateFirstName(registerPrisonerRequest.prisonerFirstName, prisonerSearchPrisoner.firstName)?.let {
        errors.add(it)
      }
      validateLastName(registerPrisonerRequest.prisonerLastName, prisonerSearchPrisoner.lastName)?.let {
        errors.add(it)
      }
      validateDob(registerPrisonerRequest.prisonerDateOfBirth, prisonerSearchPrisoner.dateOfBirth)?.let {
        errors.add(it)
      }
      validatePrisonCode(registerPrisonerRequest.prisonCode, prisonerSearchPrisoner.prisonId)?.let {
        errors.add(it)
      }
    }

    return errors.toList()
  }

  private fun validateFirstName(
    prisonerToRegisterFirstName: String,
    prisonerSearchFirstName: String?,
  ): RegisterPrisonerValidationError? = if (!areNamesMatching(prisonerToRegisterFirstName, prisonerSearchFirstName)) {
    RegisterPrisonerValidationError.FIRST_NAME_INCORRECT
  } else {
    null
  }

  private fun validateLastName(
    prisonerToRegisterLastName: String,
    prisonerSearchLastName: String?,
  ): RegisterPrisonerValidationError? = if (!areNamesMatching(prisonerToRegisterLastName, prisonerSearchLastName)) {
    RegisterPrisonerValidationError.LAST_NAME_INCORRECT
  } else {
    null
  }

  private fun areNamesMatching(
    prisonerToRegisterName: String,
    prisonerSearchName: String?,
  ): Boolean {
    val toRegisterName = sanitiseText(prisonerToRegisterName)
    val prisonApiName = prisonerSearchName?.let {
      sanitiseText(prisonerSearchName)
    }

    println("toRegisterName: $toRegisterName")
    println("prisonApiName: $prisonApiName")
    return toRegisterName.equals(prisonApiName, ignoreCase = true)
  }

  private fun sanitiseText(text: String): String = replaceSpecialCharacters(getNormalisedText(text.trim()))

  private fun replaceSpecialCharacters(
    text: String,
  ): String = RegExUtils.replacePattern(text, "[^a-zA-Z]", "")

  private fun getNormalisedText(text: String): String {
    val normalisedText = if (!Normalizer.isNormalized(text, Normalizer.Form.NFKD)) {
      StringUtils.stripAccents(text)
    } else {
      text
    }

    return normalisedText
  }

  private fun validateDob(
    prisonerToRegisterDob: LocalDate,
    prisonerSearchDob: LocalDate?,
  ): RegisterPrisonerValidationError? = if (prisonerToRegisterDob != prisonerSearchDob) {
    RegisterPrisonerValidationError.DOB_INCORRECT
  } else {
    null
  }

  private fun validatePrisonCode(
    prisonerToRegisterPrisonCode: String,
    prisonerSearchPrisonCode: String?,
  ): RegisterPrisonerValidationError? = if (!(prisonerToRegisterPrisonCode.trim().equals(prisonerSearchPrisonCode?.trim(), ignoreCase = true))) {
    RegisterPrisonerValidationError.PRISON_CODE_INCORRECT
  } else {
    null
  }

  private fun hasActivePrisoners(permittedPrisoners: List<PermittedPrisonerDto>): Boolean = permittedPrisoners.any { it.active }

  private fun isPrisonerAlreadyAdded(permittedPrisoners: List<PermittedPrisonerDto>, prisonerIdToRegister: String): Boolean = permittedPrisoners.map { it.prisonerId.uppercase() }.contains(prisonerIdToRegister.uppercase())
}
