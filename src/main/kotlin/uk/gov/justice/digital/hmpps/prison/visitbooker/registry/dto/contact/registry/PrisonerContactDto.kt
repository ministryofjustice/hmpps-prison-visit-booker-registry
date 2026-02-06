package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
data class PrisonerContactDto(
  @param:Schema(description = "Identifier for this contact (Person in NOMIS)", example = "5871791")
  val personId: Long? = null,
  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,
  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
  @param:Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,
  @param:Schema(description = "Approved Visitor Flag", required = true)
  val approvedVisitor: Boolean,
)
