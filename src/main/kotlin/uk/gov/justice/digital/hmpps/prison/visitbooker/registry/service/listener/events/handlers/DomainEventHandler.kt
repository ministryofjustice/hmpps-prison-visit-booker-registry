package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent

interface DomainEventHandler {
  val eventType: DomainEventTypes
  fun handle(domainEvent: DomainEvent)
}
