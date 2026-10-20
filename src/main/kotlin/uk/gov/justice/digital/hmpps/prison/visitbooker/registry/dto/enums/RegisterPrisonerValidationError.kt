package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class RegisterPrisonerValidationError(val telemetryEventName: String) {
  PRISONER_ALREADY_REGISTERED_AGAINST_BOOKER("prisoner-already-registered"),
  PRISONER_NOT_FOUND("prisoner-not-found"),
  LAST_NAME_INCORRECT("last-name-incorrect"),
  DOB_INCORRECT("dob-incorrect"),
  PRISON_CODE_INCORRECT("prison-code-incorrect"),
}
