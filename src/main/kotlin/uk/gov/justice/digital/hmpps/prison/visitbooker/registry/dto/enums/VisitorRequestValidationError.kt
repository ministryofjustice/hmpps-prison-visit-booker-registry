package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class VisitorRequestValidationError(
  val description: String,
) {
  PRISONER_NOT_FOUND_FOR_BOOKER("Prisoner not found on booker's permitted prisoner list"),
}
