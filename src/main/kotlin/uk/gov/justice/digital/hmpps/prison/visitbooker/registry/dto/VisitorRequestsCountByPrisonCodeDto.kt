package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema

data class VisitorRequestsCountByPrisonCodeDto(
  @param:Schema(description = "Count of visitor requests for prison", example = "5", required = true)
  val count: Int,
)
