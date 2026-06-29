package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class PrisonerMergedInfo(
  @field:NotBlank
  @param:JsonProperty("nomsNumber")
  val newPrisonerNumber: String,

  @field:NotBlank
  @param:JsonProperty("removedNomsNumber")
  val oldPrisonerNumber: String,
)
