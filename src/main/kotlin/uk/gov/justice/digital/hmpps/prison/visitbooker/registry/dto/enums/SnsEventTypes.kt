package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums

enum class SnsEventTypes(val type: String, val description: String) {
  PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED_EVENT("prison-visit-booker.visitor-approved", "Prison visit booker's prisoner visitor request approved"),
  PRISON_VISIT_BOOKER_PRISONER_VISITOR_REJECTED_EVENT("prison-visit-booker.visitor-rejected", "Prison visit booker's prisoner visitor request rejected"),
}
