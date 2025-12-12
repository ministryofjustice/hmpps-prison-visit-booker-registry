package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.LinkVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestAlreadyActionedException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository

@Service
class VisitorRequestsService(
  private val visitorRequestsRepository: VisitorRequestsRepository,
  private val bookerAuditService: BookerAuditService,
  private val bookerDetailsService: BookerDetailsService,
  private val visitorRequestsValidationService: VisitorRequestsValidationService,
  private val visitorRequestApprovalStoreService: VisitorRequestsApprovalStoreService,
  private val snsService: SnsService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun submitVisitorRequest(bookerReference: String, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto) {
    LOG.info("Entered VisitorRequestsService - submitVisitorRequest - For booker $bookerReference, prisoner $prisonerId")

    visitorRequestsValidationService.validateVisitorRequest(bookerReference, prisonerId, visitorRequest)

    bookerAuditService.auditVisitorRequest(bookerReference, prisonerId)

    visitorRequestsRepository.save(
      VisitorRequest(
        bookerReference = bookerReference,
        prisonerId = prisonerId,
        firstName = visitorRequest.firstName,
        lastName = visitorRequest.lastName,
        dateOfBirth = visitorRequest.dateOfBirth,
        status = REQUESTED,
      ),
    )
  }

  @Transactional(readOnly = true)
  fun getActiveVisitorRequests(bookerReference: String): List<BookerPrisonerVisitorRequestDto> {
    LOG.info("Entered VisitorRequestsService - getAwaitingVisitorRequests - For booker $bookerReference")
    val booker = bookerDetailsService.getBookerByReference(bookerReference)
    return visitorRequestsRepository.findAllActiveRequestsByBookerReference(booker.reference).map { BookerPrisonerVisitorRequestDto(it) }
  }

  @Transactional(readOnly = true)
  fun getVisitorRequestsCountByPrisonCode(prisonCode: String): VisitorRequestsCountByPrisonCodeDto {
    LOG.info("Entered VisitorRequestsService - getVisitorRequestsCountByPrisonCode - For prison $prisonCode")
    return VisitorRequestsCountByPrisonCodeDto(visitorRequestsRepository.findCountOfVisitorRequestsByPrisonCode(prisonCode))
  }

  @Transactional(readOnly = true)
  fun getVisitorRequestsByPrisonCode(prisonCode: String): List<PrisonVisitorRequestDto> {
    LOG.info("Entered VisitorRequestsService - getVisitorRequestsByPrisonCode - For prison $prisonCode")
    val visitorRequests = visitorRequestsRepository.findVisitorRequestsByPrisonCode(prisonCode)

    return visitorRequests.map { visitorRequest ->
      val booker = bookerDetailsService.getBookerByReference(visitorRequest.bookerReference)
      PrisonVisitorRequestDto(visitorRequest, booker.email)
    }.toList()
  }

  @Transactional(readOnly = true)
  fun getVisitorRequest(requestReference: String): PrisonVisitorRequestDto {
    LOG.info("Entered VisitorRequestsService - getVisitorRequest - requestReference $requestReference")

    val request = getVisitorRequestByReference(requestReference)
    if (request.status != REQUESTED) {
      throw VisitorRequestNotFoundException("Request not found for reference $requestReference")
    }

    val booker = bookerDetailsService.getBookerByReference(request.bookerReference)

    return PrisonVisitorRequestDto(request, booker.email)
  }

  fun approveAndLinkVisitorRequest(requestReference: String, linkVisitorRequest: LinkVisitorRequestDto): PrisonVisitorRequestDto {
    LOG.info("Entered VisitorRequestsService - approveAndLinkVisitorRequest for request reference - $requestReference, linkVisitorRequest - $linkVisitorRequest")

    val visitorRequest = getVisitorRequestByReference(requestReference)

    return when (visitorRequest.status) {
      REQUESTED -> {
        visitorRequestApprovalStoreService.approveAndLinkVisitor(bookerReference = visitorRequest.bookerReference, prisonerId = visitorRequest.prisonerId, visitorId = linkVisitorRequest.visitorId, requestReference = requestReference).also {
          // audit the event
          bookerAuditService.auditLinkVisitorApproved(bookerReference = visitorRequest.bookerReference, prisonNumber = visitorRequest.prisonerId, visitorId = linkVisitorRequest.visitorId, requestReference = requestReference)
          // send SNS event
          snsService.sendBookerPrisonerVisitorApprovedEvent(bookerReference = visitorRequest.bookerReference, prisonerId = visitorRequest.prisonerId, visitorId = linkVisitorRequest.visitorId.toString())
          LOG.info("Visitor request with reference $requestReference approved successfully")
        }
      }

      else -> {
        LOG.info("Visitor request with reference $requestReference has already been actioned. No action taken.")
        throw VisitorRequestAlreadyActionedException("Visitor request with reference $requestReference has already been actioned.")
      }
    }
  }

  private fun getVisitorRequestByReference(requestReference: String): VisitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference) ?: throw VisitorRequestNotFoundException("Request not found for reference $requestReference")
}
