package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus

data class CreateVisitorRequestResponseDto(
  @param:Schema(description = "Reference of newly created visitor request", example = "abc-def-ghi")
  val reference: String,

  @param:Schema(description = "Status of newly created visitor request", example = "AUTO_APPROVED")
  val status: VisitorRequestsStatus,

  @param:Schema(description = "Reference of booker who submitted the request", example = "abc-def-ghi")
  val bookerReference: String,

  @param:Schema(description = "The id of the booker's prisoner for the visitor request", example = "AA123456")
  val prisonerId: String,

  @param:Schema(description = "The registered prison code of the booker's prisoner for the visitor request", example = "HEI")
  val prisonId: String,
)
