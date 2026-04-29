package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ActionedByDto(
  @param:Schema(description = "STAFF username", example = "ABC123D", required = true)
  val actionedBy: String,
)
