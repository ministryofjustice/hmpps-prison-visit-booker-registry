package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers.PrisonerContactCreatedUpdatedEventHandler

@Service
class DomainEventListenerService(val prisonerContactCreatedUpdatedEventHandler: PrisonerContactCreatedUpdatedEventHandler) {
  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleMessage(domainEvent: DomainEvent) {
    when (domainEvent.eventType) {
      DomainEventTypes.PRISONER_CONTACT_CREATED_EVENT.value, DomainEventTypes.PRISONER_CONTACT_UPDATED_EVENT.value -> {
        LOG.info("Received create / update prisoner contact domain event - {}", domainEvent)
        prisonerContactCreatedUpdatedEventHandler.handle(domainEvent)
      }
      else -> {
        LOG.warn("Received unknown domain event - {}", domainEvent)
      }
    }
  }
}
