package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A contact (no prisoner relationship)")
data class ContactLinkedSocialPrisonerDto(
  @param:Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,
)
