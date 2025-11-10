package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class PrisonerDto(
  @param:Schema(required = true, description = "Prisoner Number", example = "A1234AA")
  val prisonerNumber: String,

  @param:Schema(description = "Prison ID", example = "MDI")
  val prisonId: String?,

  @param:Schema(description = "In/Out Status", example = "IN")
  var inOutStatus: String?,

  @param:Schema(required = true, description = "First Name", example = "Robert")
  var firstName: String?,

  @param:Schema(required = true, description = "Last name", example = "Larsen")
  var lastName: String?,

  @param:Schema(required = true, description = "Date of Birth", example = "1975-04-02")
  var dateOfBirth: LocalDate?,
)
