package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import java.time.LocalDateTime

@Schema(description = "Booker of visits.")
data class BookerDto(

  @param:JsonProperty("reference")
  @param:Schema(name = "reference", description = "This is the booker reference and should be used to acquire booker information", required = true)
  @field:NotBlank
  val reference: String,

  @param:JsonProperty("oneLoginSub")
  @param:Schema(name = "oneLoginSub", description = "auth reference/sub", required = true)
  val oneLoginSub: String?,

  @param:JsonProperty("email")
  @param:Schema(name = "email", description = "auth email", required = true)
  @field:NotBlank
  val email: String,

  @param:JsonProperty("permittedPrisoners")
  @param:Schema(description = "Permitted prisoners list", required = true)
  @field:Valid
  val permittedPrisoners: List<PermittedPrisonerDto>,

  @param:JsonProperty("createdTimestamp")
  @param:Schema(name = "createdTimestamp", description = "The time of booker account creation", required = true)
  @field:NotBlank
  val createdTimestamp: LocalDateTime,
) {
  constructor(booker: Booker) : this(
    reference = booker.reference,
    oneLoginSub = booker.oneLoginSub,
    email = booker.email,
    permittedPrisoners = booker.permittedPrisoners.map { PermittedPrisonerDto(it) },
    createdTimestamp = booker.createTimestamp!!,
  )
}
