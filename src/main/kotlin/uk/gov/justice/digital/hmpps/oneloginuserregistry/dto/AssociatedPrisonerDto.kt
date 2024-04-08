package uk.gov.justice.digital.hmpps.oneloginuserregistry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisoner

data class AssociatedPrisonerDto(
  @JsonProperty("prisonerNumber")
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(required = true, description = "First Name", example = "Robert")
  @JsonProperty("firstName")
  val firstName: String,

  @Schema(required = true, description = "Last name", example = "Larsen")
  @JsonProperty("lastName")
  val lastName: String,

  @Schema(required = true, description = "True if active, false otherwise.")
  @JsonProperty("isActive")
  val isActive: Boolean,
) {
  constructor(basicInfoDto: PrisonerBasicInfoDto, associatedPrisoner: AssociatedPrisoner) : this (
    prisonerNumber = associatedPrisoner.prisonerId,
    firstName = basicInfoDto.firstName,
    lastName = basicInfoDto.lastName,
    isActive = associatedPrisoner.active,
  )
}
