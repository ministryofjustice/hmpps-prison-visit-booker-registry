package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ApproveVisitorRequestDto(
  @param:Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @field:NotNull
  val visitorId: Long,

  @param:Schema(description = "STAFF username who approved the visitor", example = "ABC123D", required = true)
  @field:NotNull
  @field:NotBlank
  val actionedBy: String,
)
