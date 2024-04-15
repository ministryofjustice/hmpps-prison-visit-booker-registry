package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A contact for a prisoner")
class BasicContactDto(
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long,
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @Schema(description = "Middle name", example = "John", required = false)
  val middleName: String?,
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
)
