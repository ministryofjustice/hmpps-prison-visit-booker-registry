package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo

import jakarta.validation.constraints.NotBlank

data class PrisonerContactCreatedAdditionalInfo(
  @field:NotBlank
  val prisonerContactId: Long,
)
