package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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

    const val EVENT_PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED = "prison-visit-booker.visitor-approved"
    const val EVENT_PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED_DESC = "Prison visit booker's prisoner visitor request approved"

    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId(TOPIC_ID)
      ?: throw kotlin.RuntimeException("Topic with name $TOPIC_ID doesn't exist")
  }

  fun sendBookerPrisonerVisitorApprovedEvent(bookerReference: String, prisonerId: String, visitorId: String) {
    if (!snsEventsEnabled) {
      LOG.warn("Publish to domain events topic Disabled")
      return
    }
    LOG.info("Entered : sendBookerPrisonerVisitorApprovedEvent, for bookerReference: $bookerReference, prisonerId: $prisonerId, visitorId: $visitorId")

    val payloadEvent = HMPPSDomainEvent(
      eventType = EVENT_PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED,
      version = EVENT_PRISON_VISIT_VERSION,
      description = EVENT_PRISON_VISIT_BOOKER_PRISONER_VISITOR_APPROVED_DESC,
      occurredAt = LocalDateTime.now().toOffsetDateFormat(),
      personReference = PersonReference(identifiers = listOf(PersonIdentifier("NOMIS", prisonerId))),
      additionalInformation = AdditionalInformation(
        bookerReference = bookerReference,
        prisonerId = prisonerId,
        visitorId = visitorId,
      ),
    )

    try {
      val result = domainEventsTopic.publish(
        payloadEvent.eventType,
        objectMapper.writeValueAsString(payloadEvent),
      )

      telemetryClient.trackEvent(
        "${payloadEvent.eventType}-domain-event",
        mapOf(
          "messageId" to result.messageId(),
          "bookerReference" to payloadEvent.additionalInformation.bookerReference,
          "prisonerId" to payloadEvent.additionalInformation.prisonerId,
          "visitorId" to payloadEvent.additionalInformation.visitorId,
        ),
        null,
      )
    } catch (e: Throwable) {
      val message = "Failed (sendBookerPrisonerVisitorApprovedEvent) to publish Event $payloadEvent.eventType to $TOPIC_ID"
      LOG.error(message, e)
      throw PublishEventException(message, e)
    }
  }
}

private fun LocalDateTime.toOffsetDateFormat(): String = atZone(ZoneId.of(EVENT_ZONE_ID)).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

internal data class HMPPSDomainEvent(
  val eventType: String,
  val version: Int,
  val detailUrl: String? = null,
  val description: String,
  val occurredAt: String,
  val personReference: PersonReference,
  val additionalInformation: AdditionalInformation,
)

internal data class AdditionalInformation(
  val bookerReference: String,
  val prisonerId: String,
  val visitorId: String,
)

internal data class PersonReference(
  val identifiers: List<PersonIdentifier>,
)

internal data class PersonIdentifier(val type: String, val value: String)
