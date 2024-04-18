package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerBasicInfoDto(
  @JsonProperty("prisonerNumber")
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  val prisonerNumber: String,

  @JsonProperty("firstName")
  @Schema(description = "First Name", example = "Robert", required = true)
  val firstName: String,

  @JsonProperty("lastName")
  @Schema(description = "Last name", example = "Larsen", required = true)
  val lastName: String,
)
