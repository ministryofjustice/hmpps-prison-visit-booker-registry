package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ApproveVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes.CONTACT_UPDATED_EVENT
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsValidationService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.DomainEvent
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.Identifier

@Service
class ContactUpdatedEventHandler(
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  private val visitorRequestsService: VisitorRequestsService,
  private val visitorRequestsValidationService: VisitorRequestsValidationService,
) : DomainEventHandler {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(domainEvent: DomainEvent) {
    val contactId = domainEvent.personReference.identifiers.firstOrNull { it.type == Identifier.DPS_CONTACT_ID }?.value
    if (contactId == null) {
      LOG.error("ContactUpdatedEventHandler called for event {} with no contactId, skipping processing", domainEvent.eventType)
      return
    }

    val contactDetails = prisonerContactRegistryClient.getContact(contactId)

    val linkedPrisoners = prisonerContactRegistryClient.getContactLinkedSocialPrisoners(contactId)
    if (linkedPrisoners.isEmpty()) {
      LOG.info("ContactUpdatedEventHandler - No socially connected prisoners found for contactId: {}", contactId)
      return
    }

    for (prisoner in linkedPrisoners) {
      val prisonerNumber = prisoner.prisonerNumber

      val requests = visitorRequestsService.getActiveVisitorRequestsByPrisonerId(prisonerNumber)
      if (requests.isEmpty()) {
        LOG.info("No visitor requests found for contact linked prisoner {}", prisonerNumber)
        continue
      }

      for (request in requests) {
        val matches = visitorRequestsValidationService.matchContactNameAndDob(
          contactDetails,
          request.firstName,
          request.lastName,
          request.dateOfBirth,
        )

        if (matches) {
          LOG.info("ContactUpdatedEventHandler - Approving visitor request for prisoner {}, contactId: {}, visitor request reference: {}", prisonerNumber, contactId, request.reference)

          visitorRequestsService.approveAndLinkVisitorRequest(
            request.reference,
            ApproveVisitorRequestDto(contactDetails.contactId!!),
            autoApproval = true,
          )
        }
      }
    }
  }

  override val eventType: DomainEventTypes = CONTACT_UPDATED_EVENT
}
