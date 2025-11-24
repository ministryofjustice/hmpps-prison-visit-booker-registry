package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Create permitted prisoner with permitted visitors associated with the booker.")
data class CreatePermittedPrisonerDto(
  @param:JsonProperty("prisonerId")
  @param:Schema(description = "prisoner Id", example = "A1234AA", required = true)
  @field:NotBlank
  val prisonerId: String,

  @param:JsonProperty("prisonCode")
  @param:Schema(description = "prison code", example = "MDI", required = true)
  @field:NotBlank
  val prisonCode: String,
) {
  constructor(registerPrisonerRequestDto: RegisterPrisonerRequestDto) : this(
    prisonerId = registerPrisonerRequestDto.prisonerId,
    prisonCode = registerPrisonerRequestDto.prisonCode,
  )
}
