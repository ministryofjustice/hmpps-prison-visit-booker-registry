package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerBasicInfoDto(
  @Schema(description = "Prisoner Number", example = "A1234AA", required = true)
  val prisonerNumber: String,

  @Schema(description = "First Name", example = "Robert", required = true)
  val firstName: String,

  @Schema(description = "Last name", example = "Larsen", required = true)
  val lastName: String,
)
