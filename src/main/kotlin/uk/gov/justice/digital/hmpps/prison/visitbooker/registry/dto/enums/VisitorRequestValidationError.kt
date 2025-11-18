package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class VisitorRequestValidationError(
  val description: String,
) {
  PRISONER_NOT_FOUND_FOR_BOOKER("Prisoner not found on booker's permitted prisoner list"),
  MAX_IN_PROGRESS_REQUESTS_REACHED("Booker has maximum number of in progress requests allowed"),
  VISITOR_ALREADY_EXISTS("A visitor already exists for the given booker's prisoner"),
  REQUEST_ALREADY_EXISTS("A request already exists to add a visitor to booker's prisoner"),
}
