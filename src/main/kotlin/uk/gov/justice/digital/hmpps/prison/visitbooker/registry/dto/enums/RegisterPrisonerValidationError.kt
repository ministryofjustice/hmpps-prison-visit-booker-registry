package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class RegisterPrisonerValidationError {
  BOOKER_ALREADY_HAS_A_PRISONER,
  PRISONER_ALREADY_EXISTS_FOR_BOOKER,
  PRISONER_NOT_FOUND,
  FIRST_NAME_INCORRECT,
  LAST_NAME_INCORRECT,
  DOB_INCORRECT,
  PRISON_CODE_INCORRECT,
}
