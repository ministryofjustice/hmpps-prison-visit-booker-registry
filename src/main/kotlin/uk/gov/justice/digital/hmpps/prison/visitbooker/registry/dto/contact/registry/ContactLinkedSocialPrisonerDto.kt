package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A prisoner linked to a contact")
data class ContactLinkedSocialPrisonerDto(
  @param:Schema(description = "Prisoner number (NOMS ID)", example = "A1234BC")
  val prisonerNumber: String,
)
