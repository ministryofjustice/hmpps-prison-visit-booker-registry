package uk.gov.justice.digital.hmpps.oneloginuserregistry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail

@Schema(description = "Auth detail Dto")
data class AuthDetailDto(

  @Schema(name = "auth reference", required = true)
  @field:NotBlank
  val authReference: String,

  @Schema(name = "auth email", required = true)
  @field:NotBlank
  val authEmail: String,

  @Schema(name = "auth phone number", required = false)
  val authPhoneNumber: String? = null,

) {
  constructor(authDetail: AuthDetail) : this(
    authReference = authDetail.authReference,
    authEmail = authDetail.authEmail,
    authPhoneNumber = authDetail.authPhoneNumber,
  )
}
