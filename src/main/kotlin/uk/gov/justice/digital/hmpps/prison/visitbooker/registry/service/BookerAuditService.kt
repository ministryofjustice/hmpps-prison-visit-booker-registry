package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.ACTIVATED_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.ACTIVATED_VISITOR
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.BOOKER_CREATED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.CLEAR_BOOKER_DETAILS
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.DEACTIVATED_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.DEACTIVATED_VISITOR
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.PRISONER_ADDED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.UPDATE_BOOKER_EMAIL
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.VISITOR_ADDED_TO_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository

@Service
class BookerAuditService(
  private val telemetryClientService: TelemetryClientService,
  private val bookerAuditRepository: BookerAuditRepository,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
    private const val BOOKER_REFERENCE_PROPERTY_NAME = "bookerReference"
    private const val EMAIL_PROPERTY_NAME = "email"
    private const val PRISON_NUMBER_PROPERTY_NAME = "prisonerId"
    private const val VISITOR_ID_PROPERTY_NAME = "visitorId"
  }

  fun auditBookerCreate(bookerReference: String, email: String, hasSub: Boolean) {
    val auditType = BOOKER_CREATED
    val bookerCreatedText = if (hasSub) "Booker created" else "Booker created (without sub)"
    val text = "$bookerCreatedText with email - $email"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      EMAIL_PROPERTY_NAME to email,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditAddPrisoner(bookerReference: String, prisonNumber: String) {
    val auditType = PRISONER_ADDED
    val text = "Prisoner with prisonNumber - $prisonNumber added to booker"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditActivatePrisoner(bookerReference: String, prisonNumber: String) {
    val auditType = ACTIVATED_PRISONER
    val text = "Prisoner with prisonNumber - $prisonNumber activated"

    auditBookerEvent(bookerReference, auditType, text)
    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditDeactivatePrisoner(bookerReference: String, prisonNumber: String) {
    val auditType = DEACTIVATED_PRISONER
    val text = "Prisoner with prisonNumber - $prisonNumber deactivated"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditAddVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val auditType = VISITOR_ADDED_TO_PRISONER
    val text = "Visitor ID - $visitorId added to prisoner - $prisonNumber"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
      VISITOR_ID_PROPERTY_NAME to visitorId.toString(),
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditActivateVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val auditType = ACTIVATED_VISITOR
    val text = "Visitor ID - $visitorId activated for prisoner - $prisonNumber"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
      VISITOR_ID_PROPERTY_NAME to visitorId.toString(),
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditDeactivateVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val auditType = DEACTIVATED_VISITOR
    val text = "Visitor ID - $visitorId deactivated for prisoner - $prisonNumber"
    auditBookerEvent(bookerReference, DEACTIVATED_VISITOR, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
      VISITOR_ID_PROPERTY_NAME to visitorId.toString(),
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditClearBookerDetails(bookerReference: String) {
    val auditType = CLEAR_BOOKER_DETAILS
    val text = "Booker details cleared"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  fun auditUpdateBookerEmailAddress(bookerReference: String, oldEmail: String, newEmail: String) {
    val auditType = UPDATE_BOOKER_EMAIL
    val text = "Booker email updated from $oldEmail to $newEmail for booker reference - $bookerReference"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      "old_email" to oldEmail,
      "new_email" to newEmail,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  private fun auditBookerEvent(bookerReference: String, auditType: BookerAuditType, text: String) {
    LOG.debug("Auditing audit booker event for $bookerReference")
    bookerAuditRepository.saveAndFlush(
      BookerAudit(bookerReference = bookerReference, auditType = auditType, text = text),
    )
  }

  private fun sendTelemetryClientEvent(bookerAuditType: BookerAuditType, properties: Map<String, String>) {
    telemetryClientService.trackEvent(bookerAuditType, properties)
  }
}
