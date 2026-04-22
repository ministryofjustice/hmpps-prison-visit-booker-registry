package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes.CONTACT_UPDATED_EVENT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent

@Service
class ContactUpdatedEventHandler(
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
) : DomainEventHandler {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(domainEvent: DomainEvent) {
  }

  override val eventType: DomainEventTypes = CONTACT_UPDATED_EVENT
}
