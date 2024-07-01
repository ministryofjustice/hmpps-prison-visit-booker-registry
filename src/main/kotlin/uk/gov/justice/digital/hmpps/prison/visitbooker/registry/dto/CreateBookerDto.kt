package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Create booker with associated permitted prisoner data.")
data class CreateBookerDto(

  @JsonProperty("email")
  @Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,
)
