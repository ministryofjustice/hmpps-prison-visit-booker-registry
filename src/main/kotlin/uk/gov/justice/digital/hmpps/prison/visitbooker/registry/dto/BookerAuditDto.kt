package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import java.time.LocalDateTime

@Schema(description = "Audit entry for booker.")
data class BookerAuditDto(
  @param:Schema(name = "reference", description = "Booker reference", required = true)
  val bookerReference: String,

  @param:Schema(name = "auditType", description = "Audit Type", required = true, example = "PRISONER_REGISTERED")
  val auditType: BookerAuditType,

  @param:Schema(name = "text", description = "Audit summary", required = true)
  val text: String,

  @param:Schema(name = "createdTimestamp", description = "Timestamp of booker audit entry", required = true)
  val createdTimestamp: LocalDateTime,
) {
  constructor(bookerAudit: BookerAudit) : this(
    bookerReference = bookerAudit.bookerReference,
    auditType = bookerAudit.auditType,
    text = bookerAudit.text,
    createdTimestamp = bookerAudit.createTimestamp!!,
  )
}
