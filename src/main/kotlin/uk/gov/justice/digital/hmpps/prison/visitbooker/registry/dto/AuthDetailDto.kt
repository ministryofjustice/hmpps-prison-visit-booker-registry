package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Auth detail Dto")
data class AuthDetailDto(

  @Schema(name = "auth reference", required = true)
  @field:NotBlank
  val oneLoginSub: String,

  @Schema(name = "auth email", required = true)
  @field:NotBlank
  val email: String,

  @Schema(name = "auth phone number", required = false)
  val phoneNumber: String? = null,

)
