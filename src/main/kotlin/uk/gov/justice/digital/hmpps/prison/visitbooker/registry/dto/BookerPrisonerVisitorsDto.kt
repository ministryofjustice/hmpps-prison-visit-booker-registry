package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisonerVisitor

@Schema(description = "Prisoners associated with the booker.")
data class BookerPrisonerVisitorsDto(
  @JsonProperty("prisonerNumber")
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  @NotBlank
  val prisonerNumber: String,

  @JsonProperty("personId")
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @NotNull
  val personId: Long,

  @JsonProperty("active")
  @Schema(description = "Active / Inactive prisoner", example = "true", required = true)
  @NotNull
  val active: Boolean,
) {
  constructor(prisonerNumber: String, bookerPrisonerVisitor: BookerPrisonerVisitor) : this(
    prisonerNumber = prisonerNumber,
    personId = bookerPrisonerVisitor.visitorId,
    active = bookerPrisonerVisitor.active,
  )
}
