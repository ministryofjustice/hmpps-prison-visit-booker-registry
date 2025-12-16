package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestRejectionReason

data class RejectVisitorRequestDto(
  @param:Schema(description = "Rejection Reason type", example = "ALREADY_LINKED", required = true)
  @field:NotNull
  val rejectionReason: VisitorRequestRejectionReason,
)
