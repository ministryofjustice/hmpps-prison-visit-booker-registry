package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Create prisoner with visitors associated with the booker.")
data class CreatePrisonerDto(

  @JsonProperty("prisonerId")
  @Schema(description = "Prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @JsonProperty("visitorIds")
  @Schema(name = "visitorIds", description = "list of visitors for prisoner", required = true)
  @field:NotEmpty
  val visitorIds: List<Long>,

)
