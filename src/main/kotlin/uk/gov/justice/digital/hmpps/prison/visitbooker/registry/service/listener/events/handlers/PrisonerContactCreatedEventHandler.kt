package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.Identifier
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo.PrisonerContactCreatedAdditionalInfo

@Service
class PrisonerContactCreatedEventHandler(
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
) : DomainEventHandler {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(domainEvent: DomainEvent) {
    val additionalInfo = objectMapper.readValue(domainEvent.additionalInformation, PrisonerContactCreatedAdditionalInfo::class.java)

    LOG.info("PrisonerContactCreatedEventHandler called for event ${domainEvent.eventType}, prisoner ${domainEvent.personReference.identifiers.firstOrNull { it.type == Identifier.NOMS }?.value}, contact ${domainEvent.personReference.identifiers.firstOrNull { it.type == Identifier.DPS_CONTACT_ID }?.value} and  additional info: $additionalInfo")
  }
}
