package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Booker reference value, to be used with all other api call for booker information")
data class BookerReference(
  @Schema(name = "value", required = true)
  @field:NotBlank
  val value: String,
)
