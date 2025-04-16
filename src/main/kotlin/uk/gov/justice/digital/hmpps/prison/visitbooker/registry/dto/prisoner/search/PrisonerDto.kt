package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerDto(
  @Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @Schema(description = "In/Out Status", example = "IN")
  var inOutStatus: String?,

  @Schema(required = true, description = "First Name", example = "Robert")
  var firstName: String?,

  @Schema(required = true, description = "Last name", example = "Larsen")
  var lastName: String?,

  @Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  var dateOfBirth: LocalDate?,
)
