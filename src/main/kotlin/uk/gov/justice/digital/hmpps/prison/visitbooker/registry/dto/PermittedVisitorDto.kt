package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor

@Schema(description = "Permitted visitor associated with the permitted prisoner.")
data class PermittedVisitorDto(
  @param:JsonProperty("visitorId")
  @param:Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791", required = true)
  @field:NotNull
  val visitorId: Long,
) {
  constructor(permittedVisitor: PermittedVisitor) : this(
    visitorId = permittedVisitor.visitorId,
  )
}
