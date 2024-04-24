package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner

@Schema(description = "Prisoners associated with the booker.")
data class BookerPrisonersDto(
  @JsonProperty("prisonerNumber")
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  @NotBlank
  val prisonerNumber: String,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive prisoner", example = "true", required = true)
  @NotNull
  val active: Boolean,
) {
  constructor(bookerPrisoner: BookerPrisoner) : this(
    prisonerNumber = bookerPrisoner.prisonNumber,
    active = bookerPrisoner.active,
  )
}
