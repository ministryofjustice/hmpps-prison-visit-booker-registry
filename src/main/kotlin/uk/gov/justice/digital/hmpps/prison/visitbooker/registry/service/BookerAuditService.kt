package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.ACTIVATED_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.ACTIVATED_VISITOR
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.BOOKER_CREATED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.CLEAR_BOOKER_DETAILS
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.DEACTIVATED_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.DEACTIVATED_VISITOR
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.PRISONER_REGISTERED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.REGISTER_PRISONER_SEARCH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.UPDATE_BOOKER_EMAIL
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType.VISITOR_ADDED_TO_PRISONER
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import java.time.LocalDate

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
    private const val NEW_PRISON_CODE = "newPrisonCode"

    private interface PrisonerSearchPropertyNames {
      companion object {
        const val PRISON_NUMBER = "prisonerId"
        const val FIRST_NAME = "firstNameEntered"
        const val LAST_NAME = "lastNameEntered"
        const val DOB = "dobEntered"
        const val PRISON_CODE = "prisonCodeEntered"
        const val SUCCESS = "success"
        const val ERRORS = "failureReasons"
      }
    }
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
    val auditType = PRISONER_REGISTERED
    val text = "Prisoner with prisonNumber - $prisonNumber registered against booker"
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

  fun auditUnlinkVisitor(bookerReference: String, visitorId: Long, prisonNumber: String) {
    val text = "Visitor ID - $visitorId unlinked for prisoner - $prisonNumber, booker - $bookerReference"
    auditBookerEvent(bookerReference, BookerAuditType.UNLINK_VISITOR, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
      VISITOR_ID_PROPERTY_NAME to visitorId.toString(),
    )
    sendTelemetryClientEvent(BookerAuditType.UNLINK_VISITOR, properties)
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

  fun auditUpdateBookerPrisonerPrisonCode(bookerReference: String, prisonNumber: String, newPrisonCode: String) {
    val auditType = BookerAuditType.UPDATE_REGISTERED_PRISONER_PRISON
    val text = "Prisoner with prisonNumber - $prisonNumber had prison code updated to $newPrisonCode for booker reference - $bookerReference"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PRISON_NUMBER_PROPERTY_NAME to prisonNumber,
      NEW_PRISON_CODE to newPrisonCode,
    )
    sendTelemetryClientEvent(auditType, properties)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun auditPrisonerSearchDetails(
    bookerReference: String,
    prisonerNumber: String,
    firstName: String,
    lastName: String,
    dob: LocalDate,
    prisonCode: String,
    success: Boolean,
    failures: List<RegisterPrisonerValidationError>?,
  ) {
    val auditType = REGISTER_PRISONER_SEARCH
    val successOrFailureText = if (success) "was successful" else "failed with errors - ${failures?.joinToString { it.toString() }}"
    val text = "Prisoner search for prisonNumber - $prisonerNumber, firstName: $firstName, lastName: $lastName, DOB: $dob, prisonCode: $prisonCode $successOrFailureText"
    auditBookerEvent(bookerReference, auditType, text)

    // send event to telemetry client
    val properties = mutableMapOf(
      BOOKER_REFERENCE_PROPERTY_NAME to bookerReference,
      PrisonerSearchPropertyNames.PRISON_NUMBER to prisonerNumber,
      PrisonerSearchPropertyNames.FIRST_NAME to firstName,
      PrisonerSearchPropertyNames.LAST_NAME to lastName,
      PrisonerSearchPropertyNames.DOB to dob.toString(),
      PrisonerSearchPropertyNames.PRISON_CODE to prisonCode,
      PrisonerSearchPropertyNames.SUCCESS to success.toString(),
    )

    failures?.let {
      properties.put(PrisonerSearchPropertyNames.ERRORS, failures.joinToString { it.telemetryEventName })
    }

    sendTelemetryClientEvent(auditType, properties.toMap())
  }

  @Transactional(readOnly = true)
  fun getBookerAudit(bookerReference: String): List<BookerAudit> {
    LOG.debug("Getting booker audit entries for $bookerReference")
    val audits = bookerAuditRepository.findByBookerReference(bookerReference)
    if (audits.isEmpty()) {
      throw BookerNotFoundException("No audits found for booker, either booker doesn't exist or has no audits")
    }

    return audits
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
