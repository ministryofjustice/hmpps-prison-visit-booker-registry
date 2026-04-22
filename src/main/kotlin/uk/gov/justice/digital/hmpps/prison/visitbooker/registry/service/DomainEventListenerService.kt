package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers.DomainEventHandler

@Service
class DomainEventListenerService(
  handlers: List<DomainEventHandler>,
) {
  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(DomainEventListenerService::class.java)
  }

  private val handlersByType = handlers.associateBy { it.eventType.value }

  fun handleMessage(domainEvent: DomainEvent) {
    val handler = handlersByType[domainEvent.eventType]

    if (handler == null) {
      LOG.warn("Received unknown domain event type: {}", domainEvent.eventType)
      return
    }

    LOG.info("Received domain event type: {}", domainEvent.eventType)
    handler.handle(domainEvent)
  }
}
