package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Create permitted prisoner with permitted visitors associated with the booker.")
data class CreatePermittedVisitorDto(
  @param:Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @field:NotNull
  val visitorId: Long,

  @param:Schema(description = "A flag (boolean), when set to true will send booker notification of visitor linking", example = "true", required = false)
  @field:NotNull
  val sendNotificationFlag: Boolean? = false,
)
