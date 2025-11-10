package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner

@Schema(description = "Permitted prisoner associated with the booker.")
data class PermittedPrisonerDto(
  @param:JsonProperty("prisonerId")
  @param:Schema(description = "prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:JsonProperty("active")
  @param:Schema(description = "Active / Inactive permitted prisoner", example = "true", required = true)
  @field:NotNull
  val active: Boolean,

  @param:JsonProperty("prisonCode")
  @param:Schema(description = "prison code", example = "MDI", required = true)
  @field:NotNull
  val prisonCode: String,

  @param:JsonProperty("permittedVisitors")
  @param:Schema(description = "Permitted visitors", required = true)
  @field:Valid
  val permittedVisitors: List<PermittedVisitorDto>,
) {
  constructor(permittedPrisoner: PermittedPrisoner) : this(
    prisonerId = permittedPrisoner.prisonerId,
    active = permittedPrisoner.active,
    prisonCode = permittedPrisoner.prisonCode,
    permittedVisitors = permittedPrisoner.permittedVisitors.map { PermittedVisitorDto(it) },
  )
}
