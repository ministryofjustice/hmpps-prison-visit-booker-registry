package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Create permitted prisoner with permitted visitors associated with the booker.")
data class CreatePermittedPrisonerDto(

  @JsonProperty("prisonerId")
  @Schema(description = "prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @JsonProperty("visitorIds")
  @Schema(name = "visitorIds", description = "list of permitted visitors for permitted prisoner", required = true)
  @field:NotEmpty
  val visitorIds: List<Long>,

)
