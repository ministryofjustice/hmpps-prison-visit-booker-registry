package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class PrisonerValidationErrorCodes(
  val description: String,
) {
  PRISONER_RELEASED("Prisoner released"),
  PRISONER_TRANSFERRED("Prisoner transferred"),
}
