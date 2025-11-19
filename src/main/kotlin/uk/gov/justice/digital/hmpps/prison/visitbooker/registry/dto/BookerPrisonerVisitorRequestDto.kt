package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import java.time.LocalDate

data class BookerPrisonerVisitorRequestDto(
  @param:Schema(description = "Visitor Request reference", example = "abc-def-ghi", required = true)
  @field:NotBlank
  val reference: String,

  @param:Schema(description = "Booker reference", example = "wwe-egg-wwf", required = true)
  @field:NotBlank
  val bookerReference: String,

  @param:Schema(description = "Prisoner ID for whom visitor was requested", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:Schema(description = "First Name, as entered on visitor request", example = "John", required = true)
  @field:NotBlank
  val firstName: String,

  @param:Schema(description = "Last Name, as entered on visitor request", example = "Smith", required = true)
  @field:NotBlank
  val lastName: String,

  @param:Schema(description = "Date of birth, as entered on visitor request", example = "2000-01-01", required = true)
  val dateOfBirth: LocalDate,

  @param:Schema(description = "Request status", example = "REQUESTED", required = true)
  val status: VisitorRequestsStatus,
) {
  constructor(visitorRequest: VisitorRequest) : this(
    reference = visitorRequest.reference,
    bookerReference = visitorRequest.bookerReference,
    prisonerId = visitorRequest.prisonerId,
    firstName = visitorRequest.firstName,
    lastName = visitorRequest.lastName,
    dateOfBirth = visitorRequest.dateOfBirth,
    status = visitorRequest.status,
  )
}
