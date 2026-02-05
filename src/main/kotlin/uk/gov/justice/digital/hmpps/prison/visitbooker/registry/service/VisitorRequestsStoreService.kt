package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateVisitorRequestResponseDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestRejectionReason
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.AUTO_APPROVED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import java.time.LocalDateTime

@Service
class VisitorRequestsStoreService(
  private val visitorRepository: PermittedVisitorRepository,
  private val visitorRequestsRepository: VisitorRequestsRepository,
  private val bookerRepository: BookerRepository,
  private val visitorRequestsValidationService: VisitorRequestsValidationService,
  private val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createVisitorRequest(bookerReference: String, prisonerId: String, request: AddVisitorToBookerPrisonerRequestDto): CreateVisitorRequestResponseDto {
    LOG.info("Entered VisitorRequestsStoreService - createVisitorRequest - Booker {}, prisoner {}", bookerReference, prisonerId)

    val booker = bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker for reference : $bookerReference not found")
    val contactList = prisonerContactRegistryClient.getPrisonersApprovedSocialContacts(prisonerId)

    visitorRequestsValidationService.validateVisitorRequest(booker, prisonerId, request, contactList)

    val matchingContact = contactList.firstOrNull { contact ->
      visitorRequestsValidationService.matchContactNameAndDob(contact, request.firstName, request.lastName, request.dateOfBirth) && contact.personId != null
    }

    val visitorRequestStatus = if (matchingContact != null) {
      // Only create a visitor entry if we find a 100% match (Auto approval path)
      val bookerPrisonerEntity = booker.permittedPrisoners.first { it.prisonerId == prisonerId }

      visitorRepository.saveAndFlush(
        PermittedVisitor(
          permittedPrisonerId = bookerPrisonerEntity.id,
          permittedPrisoner = bookerPrisonerEntity,
          visitorId = matchingContact.personId!!,
        ),
      )

      AUTO_APPROVED
    } else {
      REQUESTED
    }

    val savedVisitorRequest = visitorRequestsRepository.save(
      VisitorRequest(
        bookerReference = booker.reference,
        prisonerId = prisonerId,
        firstName = request.firstName.trim(),
        lastName = request.lastName.trim(),
        dateOfBirth = request.dateOfBirth,
        status = visitorRequestStatus, // REQUESTED or AUTO_APPROVED
        visitorId = matchingContact?.personId,
      ),
    )

    // Required for auditing purposes in calling service.
    val prisonerRegisteredPrisonCode = booker.permittedPrisoners.first { it.prisonerId == prisonerId }.prisonCode

    return CreateVisitorRequestResponseDto(
      reference = savedVisitorRequest.reference,
      status = savedVisitorRequest.status,
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      prisonId = prisonerRegisteredPrisonCode,
      visitorId = matchingContact?.personId,
    )
  }

  @Transactional
  fun approveAndLinkVisitor(bookerReference: String, prisonerId: String, visitorId: Long, requestReference: String) {
    val booker = bookerRepository.findByReference(bookerReference)!!

    LOG.info("Enter VisitorRequestsApprovalStoreService approveAndLinkVisitor, booker reference - $bookerReference, prisonerId - $prisonerId, visitorId = $visitorId")
    val bookerPrisoner = booker.permittedPrisoners.firstOrNull { it.prisonerId == prisonerId } ?: throw PrisonerNotFoundException("Booker with reference $bookerReference does not have a permitted prisoner with id $prisonerId")

    visitorRepository.saveAndFlush(
      PermittedVisitor(
        permittedPrisonerId = bookerPrisoner.id,
        permittedPrisoner = bookerPrisoner,
        visitorId = visitorId,
      ),
    )
    visitorRequestsRepository.approveVisitorRequest(requestReference, visitorId, LocalDateTime.now())
  }

  @Transactional
  fun rejectVisitorRequest(bookerReference: String, prisonerId: String, requestReference: String, rejectionReason: VisitorRequestRejectionReason) {
    LOG.info("Enter VisitorRequestsApprovalStoreService rejectVisitorRequest, booker reference - $bookerReference, prisonerId - $prisonerId, requestReference = $requestReference, rejectionReason = $rejectionReason")
    visitorRequestsRepository.rejectVisitorRequest(requestReference, rejectionReason, LocalDateTime.now())
  }

  @Transactional(readOnly = true)
  fun getVisitorRequestByReference(requestReference: String): VisitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference) ?: throw VisitorRequestNotFoundException("Request not found for reference $requestReference")

  @Transactional
  fun deleteVisitorRequestsByBookerPrisonerInStatusRequested(bookerReference: String, prisonerId: String) {
    visitorRequestsRepository.deleteVisitorRequestsByBookerReferenceAndPrisonerIdInStatusRequested(bookerReference, prisonerId)
  }
}
