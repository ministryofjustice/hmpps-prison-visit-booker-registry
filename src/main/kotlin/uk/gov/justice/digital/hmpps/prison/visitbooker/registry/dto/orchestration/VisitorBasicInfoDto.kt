package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A contact for a prisoner")
data class VisitorBasicInfoDto(
  @JsonProperty("personId")
  @Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  val personId: Long,
  @JsonProperty("firstName")
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @JsonProperty("middleName")
  @Schema(description = "Middle name", example = "John", required = false)
  val middleName: String?,
  @JsonProperty("lastName")
  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
)
