package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Create booker with associated permitted prisoner data.")
data class CreateBookerDto(

  @JsonProperty("email")
  @Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,

  @JsonProperty("permittedPrisoners")
  @Schema(name = "permittedPrisoners", description = "details of permitted prisoners to visit", required = true)
  @field:NotEmpty
  @field:Valid
  val permittedPrisoners: List<CreatePermittedPrisonerDto>,

)
