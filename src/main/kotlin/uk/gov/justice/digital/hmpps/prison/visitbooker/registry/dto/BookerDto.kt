package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@Schema(description = "Booker of visits.")
data class BookerDto(

  @JsonProperty("reference")
  @Schema(name = "reference", description = "This is the booker reference and should be used to acquire booker information", required = true)
  @field:NotBlank
  val reference: String,

  @JsonProperty("oneLoginSub")
  @Schema(name = "oneLoginSub", description = "auth reference/sub", required = true)
  val oneLoginSub: String?,

  @JsonProperty("email")
  @Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,

  @JsonProperty("prisoners")
  @Schema(description = " prisoners", required = true)
  @field:Valid
  val prisoners: List<PrisonerDto>,
) {
  constructor(booker: Booker) : this(
    reference = booker.reference,
    oneLoginSub = booker.oneLoginSub,
    email = booker.email,
    prisoners = booker.prisoners.map { PrisonerDto(it) },
  )
}
