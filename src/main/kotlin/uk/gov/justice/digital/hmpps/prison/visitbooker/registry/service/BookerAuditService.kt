package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository

@Service
class BookerAuditService(
  private val bookerAuditRepository: BookerAuditRepository,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun auditBookerCreate(bookerReference: String, email: String, hasSub: Boolean) {
    val bookerCreatedText = if (hasSub) "Booker created" else "Booker created (without sub)"
    val text = "$bookerCreatedText with email - $email"
    auditBookerEvent(bookerReference, BookerAuditType.BOOKER_CREATED, text)
  }

  fun auditAddPrisoner(bookerReference: String, prisonNumber: String) {
    val text = "Prisoner with prisonNumber - $prisonNumber added to booker"
    auditBookerEvent(bookerReference, BookerAuditType.PRISONER_ADDED, text)
  }

  fun auditActivatePrisoner(bookerReference: String, prisonNumber: String) {
    val text = "Prisoner with prisonNumber - $prisonNumber activated"
    auditBookerEvent(bookerReference, BookerAuditType.ACTIVATED_PRISONER, text)
  }

  fun auditDeactivatePrisoner(bookerReference: String, prisonNumber: String) {
    val text = "Prisoner with prisonNumber - $prisonNumber deactivated"
    auditBookerEvent(bookerReference, BookerAuditType.DEACTIVATED_PRISONER, text)
  }

  fun auditAddVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val text = "Visitor ID - $visitorId added to prisoner - $prisonNumber"
    auditBookerEvent(bookerReference, BookerAuditType.VISITOR_ADDED_TO_PRISONER, text)
  }

  fun auditActivateVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val text = "Visitor ID - $visitorId activated for prisoner - $prisonNumber"
    auditBookerEvent(bookerReference, BookerAuditType.ACTIVATED_VISITOR, text)
  }

  fun auditDeactivateVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val text = "Visitor ID - $visitorId deactivated for prisoner - $prisonNumber"
    auditBookerEvent(bookerReference, BookerAuditType.DEACTIVATED_VISITOR, text)
  }

  fun auditClearBookerDetails(bookerReference: String) {
    val text = "Booker details cleared"
    auditBookerEvent(bookerReference, BookerAuditType.CLEAR_BOOKER_DETAILS, text)
  }

  private fun auditBookerEvent(bookerReference: String, auditType: BookerAuditType, text: String) {
    LOG.debug("Auditing audit booker event for $bookerReference")
    bookerAuditRepository.saveAndFlush(
      BookerAudit(bookerReference = bookerReference, auditType = auditType, text = text),
    )
  }
}
