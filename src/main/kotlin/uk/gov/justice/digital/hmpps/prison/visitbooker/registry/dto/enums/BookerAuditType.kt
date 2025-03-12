package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class BookerAuditType(val telemetryEventName: String) {
  BOOKER_CREATED("booker_created"),
  PRISONER_ADDED("prisoner_added"),
  VISITOR_ADDED_TO_PRISONER("visitor_added"),
  ACTIVATED_PRISONER("prisoner_activated"),
  DEACTIVATED_PRISONER("prisoner_deactivated"),
  ACTIVATED_VISITOR("visitor_activated"),
  DEACTIVATED_VISITOR("visitor_deactivated"),
  CLEAR_BOOKER_DETAILS("booker_details_cleared"),
  UPDATE_BOOKER_EMAIL("booker_email_updated"),
}
