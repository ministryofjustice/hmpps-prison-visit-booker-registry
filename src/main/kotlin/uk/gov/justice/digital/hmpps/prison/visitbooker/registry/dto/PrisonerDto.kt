package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Prisoner

@Schema(description = "Prisoner associated with the booker.")
data class PrisonerDto(
  @JsonProperty("prisonerId")
  @Schema(description = "Prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive prisoner", example = "true", required = true)
  @field:NotNull
  val active: Boolean,

  @JsonProperty("visitors")
  @Schema(description = " visitors", required = true)
  @field:Valid
  val visitors: List<VisitorDto>,
) {
  constructor(prisoner: Prisoner) : this(
    prisonerId = prisoner.prisonerId,
    active = prisoner.active,
    visitors = prisoner.visitors.map { VisitorDto(it) },
  )
}
