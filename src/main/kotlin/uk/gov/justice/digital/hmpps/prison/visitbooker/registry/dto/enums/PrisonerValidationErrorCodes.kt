package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class PrisonerValidationErrorCodes(
  val description: String,
) {
  PRISONER_RELEASED("Prisoner released"),
  PRISONER_TRANSFERRED_SUPPORTED_PRISON("Prisoner transferred to a supported prison"),
  PRISONER_TRANSFERRED_UNSUPPORTED_PRISON("Prisoner transferred to an unsupported prison"),
}
