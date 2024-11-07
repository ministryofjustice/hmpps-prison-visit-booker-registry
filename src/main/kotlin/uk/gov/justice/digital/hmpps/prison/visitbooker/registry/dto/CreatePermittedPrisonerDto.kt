package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Create permitted prisoner with permitted visitors associated with the booker.")
data class CreatePermittedPrisonerDto(

  @JsonProperty("prisonerId")
  @Schema(description = "prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @JsonProperty("prisonCode")
  @Schema(description = "prison code", example = "MDI", required = true)
  @field:NotBlank
  val prisonCode: String,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive permitted prisoner", example = "true", required = true)
  @field:NotNull
  val active: Boolean,

)
