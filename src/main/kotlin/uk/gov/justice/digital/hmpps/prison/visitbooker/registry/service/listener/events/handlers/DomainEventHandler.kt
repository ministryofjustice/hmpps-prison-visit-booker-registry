package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent

interface DomainEventHandler {
  fun handle(domainEvent: DomainEvent)
}
