package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ApproveVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsValidationService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.Identifier
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo.PrisonerContactCreatedAdditionalInfo

@Service
class PrisonerContactCreatedEventHandler(
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  private val visitorRequestsService: VisitorRequestsService,
  private val visitorRequestsValidationService: VisitorRequestsValidationService,
) : DomainEventHandler {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(domainEvent: DomainEvent) {
    val prisonerId = domainEvent.personReference.identifiers.firstOrNull { it.type == Identifier.NOMS }?.value
    val contactId = domainEvent.personReference.identifiers.firstOrNull { it.type == Identifier.DPS_CONTACT_ID }?.value
    val relationshipId = objectMapper.readValue(domainEvent.additionalInformation, PrisonerContactCreatedAdditionalInfo::class.java).prisonerContactId

    LOG.info("PrisonerContactCreatedEventHandler called for event ${domainEvent.eventType}, prisoner $prisonerId, contact $contactId and relationship (prisonerContactId) $relationshipId")

    if (prisonerId == null || contactId == null) {
      LOG.error("PrisonerContactCreatedEventHandler called for event ${domainEvent.eventType} with no prisonerId ($prisonerId) or contactId ($contactId), skipping processing of event")
      return
    }

    val requests = visitorRequestsService.getActiveVisitorRequestsByPrisonerId(prisonerId)
    if (requests.isEmpty()) {
      LOG.info("PrisonerContactCreatedEventHandler - No visitor requests found for prisoner $prisonerId")
      return
    }

    val contactDetails = prisonerContactRegistryClient.getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
    if (contactDetails.contactType != "S") {
      LOG.info("Skipping processing of event as contact is not a social contact, prisonerId: $prisonerId, contactId: $contactId, relationshipId: $relationshipId, contactType: ${contactDetails.contactType}")
      return
    }

    requests.forEach { request ->
      if (visitorRequestsValidationService.matchContactNameAndDob(contactDetails, request.firstName, request.lastName, request.dateOfBirth)) {
        LOG.info("PrisonerContactCreatedEventHandler - Approving visitor request for prisoner $prisonerId, contactId: $contactId, relationshipId: $relationshipId, visitor request reference: ${request.reference}")
        visitorRequestsService.approveAndLinkVisitorRequest(request.reference, ApproveVisitorRequestDto(contactDetails.personId!!), autoApproval = true)
      }
    }
  }
}
