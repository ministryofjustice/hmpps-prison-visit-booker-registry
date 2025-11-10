package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Booker of visits.")
data class SearchBookerDto(
  @param:JsonProperty("email")
  @param:Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,
)
