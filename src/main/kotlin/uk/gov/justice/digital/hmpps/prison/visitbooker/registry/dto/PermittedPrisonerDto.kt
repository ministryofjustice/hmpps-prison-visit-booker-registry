package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner

@Schema(description = "Permitted prisoner associated with the booker.")
data class PermittedPrisonerDto(
  @JsonProperty("prisonerId")
  @Schema(description = "prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive permitted prisoner", example = "true", required = true)
  @field:NotNull
  val active: Boolean,

  @JsonProperty("permittedVisitors")
  @Schema(description = "Permitted visitors", required = true)
  @field:Valid
  val permittedVisitors: List<PermittedVisitorDto>,
) {
  constructor(permittedPrisoner: PermittedPrisoner) : this(
    prisonerId = permittedPrisoner.prisonerId,
    active = permittedPrisoner.active,
    permittedVisitors = permittedPrisoner.permittedVisitors.map { PermittedVisitorDto(it) },
  )
}
