package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository

@Transactional(readOnly = true)
@Service
class VisitorRequestsValidationService(
  private val visitorRequestsRepository: VisitorRequestsRepository,
  private val bookerDetailsService: BookerDetailsService,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  @param:Value("\${visitor-requests.request-limit}") private val maximumRequestsLimit: Int,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun validateVisitorRequest(bookerReference: String, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto) {
    LOG.info("Entered VisitorRequestsService - validateVisitorRequest - For booker $bookerReference, prisoner $prisonerId")

    val booker = bookerDetailsService.getBookerByReference(bookerReference)
    validateBookerPrisonerRelationship(booker, prisonerId)

    val existingRequests = visitorRequestsRepository.findAllActiveRequestsByBookerReference(bookerReference)
    validateMaximumAllowedRequestsInProgress(booker, existingRequests)
    validateDuplicateRequest(visitorRequest, existingRequests = existingRequests.filter { it.prisonerId == prisonerId })

    validateVisitorAlreadyAdded(booker, prisonerId, visitorRequest)

    LOG.info("Successfully validated visitor request - For booker $bookerReference, prisoner $prisonerId")
  }

  private fun validateBookerPrisonerRelationship(booker: BookerDto, prisonerId: String) {
    if (!(booker.permittedPrisoners.any { it.prisonerId == prisonerId })) {
      LOG.error("Booker with reference ${booker.reference} does not have a permitted prisoner with id $prisonerId")
      throw VisitorRequestValidationException(VisitorRequestValidationError.PRISONER_NOT_FOUND_FOR_BOOKER)
    }
  }

  private fun validateMaximumAllowedRequestsInProgress(booker: BookerDto, existingRequests: List<VisitorRequest>) {
    if (existingRequests.count() >= maximumRequestsLimit) {
      LOG.error("Booker with reference ${booker.reference} has maximum amount of requests in progress allowed")
      throw VisitorRequestValidationException(VisitorRequestValidationError.MAX_IN_PROGRESS_REQUESTS_REACHED)
    }
  }

  private fun validateDuplicateRequest(visitorRequest: AddVisitorToBookerPrisonerRequestDto, existingRequests: List<VisitorRequest>) {
    existingRequests.forEach { existingRequest ->
      if (existingRequest.firstName == visitorRequest.firstName &&
        existingRequest.lastName == visitorRequest.lastName &&
        existingRequest.dateOfBirth == visitorRequest.dateOfBirth
      ) {
        throw VisitorRequestValidationException(VisitorRequestValidationError.REQUEST_ALREADY_EXISTS)
      }
    }
  }

  private fun validateVisitorAlreadyAdded(booker: BookerDto, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto) {
    prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerId).forEach { contact ->
      if (contact.firstName == visitorRequest.firstName &&
        contact.lastName == visitorRequest.lastName &&
        contact.dateOfBirth == visitorRequest.dateOfBirth
      ) {
        if (booker.permittedPrisoners.first { it.prisonerId == prisonerId }.permittedVisitors.any { it.visitorId == contact.personId }) {
          throw VisitorRequestValidationException(VisitorRequestValidationError.VISITOR_ALREADY_EXISTS)
        }
      }
    }
  }
}
