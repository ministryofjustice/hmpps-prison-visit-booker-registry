package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Prisoner merge request with old and new prisoner numbers.")
data class PrisonerMergeRequestDto(
  @param:JsonProperty("oldPrisonerNumber")
  @param:JsonAlias("removedNomsNumber")
  @param:Schema(description = "Old prisoner number to replace.", example = "A1234AA", required = true)
  @field:NotBlank
  val oldPrisonerNumber: String,

  @param:JsonProperty("newPrisonerNumber")
  @param:JsonAlias("nomsNumber")
  @param:Schema(description = "New prisoner number to update records to.", example = "B1234BB", required = true)
  @field:NotBlank
  val newPrisonerNumber: String,
)

@Schema(description = "Batch prisoner merge request.")
data class PrisonerMergeBatchRequestDto(
  @param:JsonProperty("prisonerMerges")
  @param:Schema(description = "Prisoner merges to process.", required = true)
  @field:NotEmpty
  @field:Valid
  val prisonerMerges: List<PrisonerMergeRequestDto>,
)
