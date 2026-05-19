package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.ContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class VisitorRequestsValidationService(
  private val visitorRequestsRepository: VisitorRequestsRepository,
  private val stringInputUtils: StringInputUtils,
  @param:Value("\${visitor-requests.request-limit}") private val maximumRequestsLimit: Int,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun validateVisitorRequest(
    booker: Booker,
    prisonerId: String,
    visitorRequest: AddVisitorToBookerPrisonerRequestDto,
    prisonerContactList: List<PrisonerContactDto>,
  ) {
    LOG.info("Entered VisitorRequestsService - validateVisitorRequest - For booker ${booker.reference}, prisoner $prisonerId")

    validateBookerPrisonerRelationship(booker, prisonerId)

    val existingRequests = visitorRequestsRepository.findAllActiveRequestsByBookerReference(booker.reference)

    validateMaximumAllowedRequestsInProgress(booker, existingRequests)
    validateDuplicateRequest(visitorRequest, existingRequests = existingRequests.filter { it.prisonerId == prisonerId })
    validateVisitorAlreadyAdded(booker, prisonerId, visitorRequest, prisonerContactList)

    LOG.info("Successfully validated visitor request - For booker ${booker.reference}, prisoner $prisonerId")
  }

  fun hasMultipleMatchingContacts(
    contacts: List<PrisonerContactDto>,
    lastName: String,
    dateOfBirth: LocalDate?,
  ): Boolean = contacts.count { contact ->
    isEligibleMatchingContact(contact) && matchContactLastNameAndDob(contact, lastName, dateOfBirth)
  } > 1

  private fun isEligibleMatchingContact(contact: PrisonerContactDto): Boolean = contact.personId != null

  fun matchContactLastNameAndDob(
    contact: PrisonerContactDto,
    lastName: String,
    dateOfBirth: LocalDate?,
  ): Boolean = matchLastNameAndDob(
    contact.lastName,
    contact.dateOfBirth,
    lastName,
    dateOfBirth,
  )

  fun matchContactLastNameAndDob(
    contact: ContactDto,
    lastName: String,
    dateOfBirth: LocalDate?,
  ): Boolean = matchLastNameAndDob(
    contact.lastName,
    contact.dateOfBirth,
    lastName,
    dateOfBirth,
  )

  private fun matchLastNameAndDob(
    contactLastName: String,
    contactDateOfBirth: LocalDate? = null,
    lastName: String,
    dateOfBirth: LocalDate?,
  ): Boolean {
    val contactLast = stringInputUtils.sanitiseText(contactLastName)
    val inputLast = stringInputUtils.sanitiseText(lastName)

    return contactLast.equals(inputLast, ignoreCase = true) &&
      contactDateOfBirth == dateOfBirth
  }

  private fun validateBookerPrisonerRelationship(booker: Booker, prisonerId: String) {
    if (!(booker.permittedPrisoners.any { it.prisonerId == prisonerId })) {
      LOG.error("Booker with reference ${booker.reference} does not have a permitted prisoner with id $prisonerId")
      throw VisitorRequestValidationException(VisitorRequestValidationError.PRISONER_NOT_FOUND_FOR_BOOKER)
    }
  }

  private fun validateMaximumAllowedRequestsInProgress(booker: Booker, existingRequests: List<VisitorRequest>) {
    if (existingRequests.count() >= maximumRequestsLimit) {
      LOG.error("Booker with reference ${booker.reference} has maximum amount of requests in progress allowed")
      throw VisitorRequestValidationException(VisitorRequestValidationError.MAX_IN_PROGRESS_REQUESTS_REACHED)
    }
  }

  private fun validateDuplicateRequest(visitorRequest: AddVisitorToBookerPrisonerRequestDto, existingRequests: List<VisitorRequest>) {
    val inputFirst = stringInputUtils.sanitiseText(visitorRequest.firstName)
    val inputLast = stringInputUtils.sanitiseText(visitorRequest.lastName)
    val inputDob = visitorRequest.dateOfBirth

    existingRequests.forEach { existing ->
      val existingFirst = stringInputUtils.sanitiseText(existing.firstName)
      val existingLast = stringInputUtils.sanitiseText(existing.lastName)

      val isDuplicate = existingFirst.equals(inputFirst, ignoreCase = true) &&
        existingLast.equals(inputLast, ignoreCase = true) &&
        existing.dateOfBirth == inputDob

      if (isDuplicate) {
        throw VisitorRequestValidationException(VisitorRequestValidationError.REQUEST_ALREADY_EXISTS)
      }
    }
  }

  private fun validateVisitorAlreadyAdded(booker: Booker, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto, prisonerContactList: List<PrisonerContactDto>) {
    val permittedVisitorIds = booker.permittedPrisoners
      .first { it.prisonerId == prisonerId }
      .permittedVisitors
      .map { it.visitorId }
      .distinct()
      .toList()

    val visitorAlreadyExists = prisonerContactList.any { contact ->
      contact.personId in permittedVisitorIds && matchContactLastNameAndDob(
        contact,
        visitorRequest.lastName,
        visitorRequest.dateOfBirth,
      )
    }

    if (visitorAlreadyExists) {
      throw VisitorRequestValidationException(VisitorRequestValidationError.VISITOR_ALREADY_EXISTS)
    }
  }
}
