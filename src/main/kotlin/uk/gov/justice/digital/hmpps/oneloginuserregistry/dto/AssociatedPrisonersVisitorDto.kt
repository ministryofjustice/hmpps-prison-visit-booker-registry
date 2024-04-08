package uk.gov.justice.digital.hmpps.oneloginuserregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageprisonvisitsorchestration.dto.contact.registry.BasicContactDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisonersVisitor

class AssociatedPrisonersVisitorDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long,
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @Schema(description = "Middle name", example = "Mark", required = false)
  val middleName: String? = null,
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @Schema(required = true, description = "True if active, false otherwise.")
  val isActive: Boolean,
) {
  constructor(contactDto: BasicContactDto, associatedPrisonersVisitor: AssociatedPrisonersVisitor) : this (
    personId = associatedPrisonersVisitor.visitorId,
    firstName = contactDto.firstName,
    middleName = contactDto.middleName,
    lastName = contactDto.lastName,
    isActive = associatedPrisonersVisitor.active,
  )
}
