package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.SnsEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.SnsEventTypes.PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED_EVENT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.SnsEventTypes.PRISON_VISIT_BOOKER_PRISONER_VISITOR_REJECTED_EVENT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestRejectionReason
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PublishEventException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.SnsService.Companion.EVENT_ZONE_ID
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.publish
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SnsService(
  private val hmppsQueueService: HmppsQueueService,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
  @param:Value("\${feature.events.sns.enabled:true}")
  private val snsEventsEnabled: Boolean,
) {

  companion object {
    const val TOPIC_ID = "domainevents"
    const val EVENT_ZONE_ID = "Europe/London"
    const val EVENT_PRISON_VISIT_VERSION = 1

    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID)
      ?: throw kotlin.RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  fun sendBookerPrisonerVisitorApprovedEvent(bookerReference: String, prisonerId: String, visitorId: String) {
    LOG.info("Entered : sendBookerPrisonerVisitorApprovedEvent, for bookerReference: $bookerReference, prisonerId: $prisonerId, visitorId: $visitorId")
    val additionalInformation = ApprovedAdditionalInformation(
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      visitorId = visitorId,
    )

    val payloadEvent = getPayloadEvent(PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED_EVENT, prisonerId, additionalInformation)

    sendDomainEvent(payloadEvent)?.let {
      telemetryClient.trackEvent(
        "${payloadEvent.eventType.type}-domain-event",
        mapOf(
          "messageId" to it.messageId(),
          "bookerReference" to additionalInformation.bookerReference,
          "prisonerId" to additionalInformation.prisonerId,
          "visitorId" to additionalInformation.visitorId,
        ),
        null,
      )
    }
  }

  fun sendVisitorRequestRejectedEvent(bookerReference: String, prisonerId: String, rejectionReason: VisitorRequestRejectionReason) {
    LOG.info("Entered : sendVisitorRequestRejectedAsRejectedEvent, for bookerReference: $bookerReference, prisonerId: $prisonerId")
    val additionalInformation = RejectedAdditionalInformation(
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      // TODO not setting the rejection reason for now
      // rejectionReason = rejectionReason,
    )

    val payloadEvent = getPayloadEvent(PRISON_VISIT_BOOKER_PRISONER_VISITOR_REJECTED_EVENT, prisonerId, additionalInformation)

    sendDomainEvent(payloadEvent)?.let {
      telemetryClient.trackEvent(
        "${payloadEvent.eventType.type}-domain-event",
        mapOf(
          "messageId" to it.messageId(),
          "bookerReference" to additionalInformation.bookerReference,
          "prisonerId" to additionalInformation.prisonerId,
          "rejectionReason" to rejectionReason.name,
        ),
        null,
      )
    }
  }

  private fun getPayloadEvent(eventType: SnsEventTypes, prisonerId: String, additionalInformation: AdditionalInformation) = HMPPSDomainEvent(
    eventType = eventType,
    version = EVENT_PRISON_VISIT_VERSION,
    description = eventType.description,
    occurredAt = LocalDateTime.now().toOffsetDateFormat(),
    personReference = PersonReference(identifiers = listOf(PersonIdentifier("NOMIS", prisonerId))),
    additionalInformation = additionalInformation,
  )

  private fun sendDomainEvent(payloadEvent: HMPPSDomainEvent): PublishResponse? {
    if (!snsEventsEnabled) {
      LOG.warn("Publish to domain events topic Disabled")
      return null
    }
    try {
      return domainEventsTopic.publish(
        payloadEvent.eventType.type,
        objectMapper.writeValueAsString(payloadEvent),
      )
    } catch (e: Throwable) {
      val message =
        "Failed (sendBookerPrisonerVisitorApprovedEvent) to publish Event $payloadEvent.eventType to $TOPIC_ID"
      LOG.error(message, e)
      throw PublishEventException(message, e)
    }
  }
}

private fun LocalDateTime.toOffsetDateFormat(): String = atZone(ZoneId.of(EVENT_ZONE_ID)).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

internal data class HMPPSDomainEvent(
  val eventType: SnsEventTypes,
  val version: Int,
  val detailUrl: String? = null,
  val description: String,
  val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: AdditionalInformation,
)

interface AdditionalInformation

internal data class ApprovedAdditionalInformation(
  val bookerReference: String,
  val prisonerId: String,
  val visitorId: String,
) : AdditionalInformation

internal data class RejectedAdditionalInformation(
  val bookerReference: String,
  val prisonerId: String,
) : AdditionalInformation

internal data class PersonReference(
  val identifiers: List<PersonIdentifier>,
)

internal data class PersonIdentifier(val type: String, val value: String)
