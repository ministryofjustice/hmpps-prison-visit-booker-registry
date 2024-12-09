package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerDto(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @Schema(description = "In/Out Status", example = "IN")
  var inOutStatus: String?,
)
