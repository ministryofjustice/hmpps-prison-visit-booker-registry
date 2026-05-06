package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ActionedByDto(
  @param:Schema(description = "STAFF username", example = "ABC123D", required = true)
  @field:NotNull
  @field:NotBlank
  val actionedBy: String,
)
