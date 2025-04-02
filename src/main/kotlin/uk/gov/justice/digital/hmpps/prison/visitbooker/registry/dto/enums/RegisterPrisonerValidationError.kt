package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class RegisterPrisonerValidationError(val telemetryEventName: String) {
  // TODO - to be removed when we allow multiple prisoners.
  BOOKER_ALREADY_HAS_A_PRISONER("booker-has-active-prisoner"),
  PRISONER_ALREADY_EXISTS_FOR_BOOKER("prisoner-already-added"),
  PRISONER_NOT_FOUND("prisoner-not-found"),
  FIRST_NAME_INCORRECT("first-name-incorrect"),
  LAST_NAME_INCORRECT("last-name-incorrect"),
  DOB_INCORRECT("dob-incorrect"),
  PRISON_CODE_INCORRECT("prison-code-incorrect"),
}
