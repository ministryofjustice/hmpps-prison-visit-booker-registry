package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.SQSMessage

@Service
class DomainEventListener(
  private val domainEventListenerService: DomainEventListenerService,
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
  @param:Value("\${domain-event-processing.enabled}") val domainEventProcessingEnabled: Boolean,
) {
  companion object {
    const val PRISON_VISITS_CREATE_CONTACT_EVENT_QUEUE_CONFIG_KEY = "prisonvisitscreatecontactevent"
    private val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener(PRISON_VISITS_CREATE_CONTACT_EVENT_QUEUE_CONFIG_KEY, factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(sqsMessage: SQSMessage) {
    if (!domainEventProcessingEnabled) {
      LOG.debug("Domain event processing is disabled")
      return
    }

    val event = objectMapper.readValue(sqsMessage.message, DomainEvent::class.java)
    domainEventListenerService.handleMessage(event)
  }
}
