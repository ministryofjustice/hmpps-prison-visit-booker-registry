package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Create permitted prisoner with permitted visitors associated with the booker.")
data class CreatePermittedVisitorDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @NotNull
  val visitorId: Long,

  @Schema(description = "Active / Inactive permitted visitor", example = "true", required = true)
  @NotNull
  val active: Boolean,

  @Schema(description = "A flag (boolean), when set to true will send booker notification of visitor linking", example = "true", required = false)
  @NotNull
  val sendNotificationFlag: Boolean? = false,
)
