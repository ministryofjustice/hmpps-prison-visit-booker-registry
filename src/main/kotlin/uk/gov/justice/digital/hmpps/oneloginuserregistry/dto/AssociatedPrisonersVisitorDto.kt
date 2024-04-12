package uk.gov.justice.digital.hmpps.oneloginuserregistry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.orchestration.BasicContactDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.BookerPrisonerVisitor

class AssociatedPrisonersVisitorDto(
  @JsonProperty("personId")
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long,
  @JsonProperty("firstName")
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @JsonProperty("middleName")
  @Schema(description = "Middle name", example = "Mark", required = false)
  val middleName: String? = null,
  @JsonProperty("lastName")
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @JsonProperty("isActive")
  @Schema(required = true, description = "True if active, false otherwise.")
  val isActive: Boolean,
) {
  constructor(contactDto: BasicContactDto, bookerPrisonerVisitor: BookerPrisonerVisitor) : this (
    personId = bookerPrisonerVisitor.visitorId,
    firstName = contactDto.firstName,
    middleName = contactDto.middleName,
    lastName = contactDto.lastName,
    isActive = bookerPrisonerVisitor.active,
  )
}
