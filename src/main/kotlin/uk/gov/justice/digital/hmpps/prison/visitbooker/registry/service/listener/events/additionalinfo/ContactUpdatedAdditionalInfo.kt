package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo

import jakarta.validation.constraints.NotNull

data class ContactUpdatedAdditionalInfo(
  @field:NotNull
  val contactId: Long,
)
