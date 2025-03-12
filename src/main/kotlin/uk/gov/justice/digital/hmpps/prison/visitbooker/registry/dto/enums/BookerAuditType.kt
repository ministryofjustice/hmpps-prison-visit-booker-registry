package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class BookerAuditType {
  BOOKER_CREATED,
  PRISONER_ADDED,
  VISITOR_ADDED_TO_PRISONER,
  ACTIVATED_PRISONER,
  DEACTIVATED_PRISONER,
  ACTIVATED_VISITOR,
  DEACTIVATED_VISITOR,
  CLEAR_BOOKER_DETAILS,
}
