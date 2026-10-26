package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditAttributeTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAuditAttribute

data class BookerAuditAttributeDto(
  @param:Schema(description = "Name of the attribute associated with the booker audit", example = "PRISONER_NUMBER", required = true)
  @field:NotNull
  val attributeName: BookerAuditAttributeTypes,

  @param:Schema(description = "Value of the attribute associated with the booker audit", example = "AB1234RE", required = true)
  @field:NotNull
  val attributeValue: String,
) {
  constructor(bookerAuditAttribute: BookerAuditAttribute) : this (
    attributeName = bookerAuditAttribute.attributeName,
    attributeValue = bookerAuditAttribute.attributeValue,
  )
}
