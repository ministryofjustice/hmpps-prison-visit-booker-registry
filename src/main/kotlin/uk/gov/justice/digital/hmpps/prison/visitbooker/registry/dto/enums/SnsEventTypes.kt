package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class SnsEventTypes(val type: String, val description: String) {
  PRISON_VISIT_BOOKER_PRISONER_VISITOR_LINKED_EVENT("prison-visit-booker.visitor-linked", "Prison visit booker's prisoner visitor was linked"),
  PRISON_VISIT_BOOKER_PRISONER_VISITOR_REQUEST_APPROVED_EVENT("prison-visit-booker.visitor-request-approved", "Prison visit booker's prisoner visitor request approved"),
  PRISON_VISIT_BOOKER_PRISONER_VISITOR_REQUEST_REJECTED_EVENT("prison-visit-booker.visitor-request-rejected", "Prison visit booker's prisoner visitor request rejected"),
}
