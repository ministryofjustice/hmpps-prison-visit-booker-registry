package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.PrisonerMergeService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo.PrisonerMergedInfo

@Service
class PrisonerMergeEventHandler(
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,

  private val prisonerMergeService: PrisonerMergeService,
) : DomainEventHandler {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(domainEvent: DomainEvent) {
    val prisonerMergedInfo = objectMapper.readValue(domainEvent.additionalInformation, PrisonerMergedInfo::class.java)
    val oldPrisonerNumber = prisonerMergedInfo.oldPrisonerNumber
    val newPrisonerNumber = prisonerMergedInfo.newPrisonerNumber

    LOG.info("Handling prisoner merge event, old prisoner number - {}, new prisoner number - {}", oldPrisonerNumber, newPrisonerNumber)
    prisonerMergeService.mergePrisoner(oldPrisonerNumber, newPrisonerNumber)
  }

  override val eventType: DomainEventTypes = DomainEventTypes.PRISONER_MERGE_EVENT
}
