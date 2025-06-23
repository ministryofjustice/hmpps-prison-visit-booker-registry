package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Update a prisoner's prison code.")
data class UpdatePrisonersPrisonDto(
  @JsonProperty("prisonId")
  @field:NotBlank
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
)
