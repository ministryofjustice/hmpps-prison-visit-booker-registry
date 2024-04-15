package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

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
)
