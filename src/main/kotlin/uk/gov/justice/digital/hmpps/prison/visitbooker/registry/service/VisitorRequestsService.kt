package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.LinkVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.APPROVED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REJECTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus.REQUESTED
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestAlreadyApprovedException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestAlreadyRejectedException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import java.time.LocalDateTime

@Service
class VisitorRequestsService(
  private val visitorRequestsRepository: VisitorRequestsRepository,
  private val bookerAuditService: BookerAuditService,
  private val bookerDetailsService: BookerDetailsService,
  private val visitorRequestsValidationService: VisitorRequestsValidationService,
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

    val request = visitorRequestsRepository.findVisitorRequestByReference(requestReference)
    if (request == null || request.status != REQUESTED) {
      throw VisitorRequestNotFoundException("Request not found for reference $requestReference")
    }

    val booker = bookerDetailsService.getBookerByReference(request.bookerReference)

    return PrisonVisitorRequestDto(request, booker.email)
  }

  fun approveAndLinkVisitorRequest(requestReference: String, linkVisitorRequest: LinkVisitorRequestDto) {
    LOG.info("Entered VisitorRequestsService - approveAndLinkVisitorRequest for request reference - $requestReference, linkVisitorRequest - $linkVisitorRequest")

    val visitorRequest = getVisitorRequestByReference(requestReference)

    when (visitorRequest.status) {
      REQUESTED -> {
        approveAndLinkVisitor(visitorRequest.bookerReference, visitorRequest.prisonerId, requestReference, linkVisitorRequest)
        LOG.info("Visitor request with reference $requestReference approved successfully")
      }

      REJECTED -> {
        LOG.info("Visitor request with reference $requestReference has already been rejected. No action taken.")
        throw VisitorRequestAlreadyRejectedException("Visitor request with reference $requestReference has already been rejected.")
      }

      APPROVED -> {
        LOG.info("Visitor request with reference $requestReference has already been approved. No action taken.")
        throw VisitorRequestAlreadyApprovedException("Visitor request with reference $requestReference has already been approved.")
      }
    }
  }

  private fun approveAndLinkVisitor(bookerReference: String, prisonerId: String, requestReference: String, linkVisitorRequest: LinkVisitorRequestDto) {
    val booker = bookerDetailsService.getBookerByReference(bookerReference)
    bookerDetailsService.createBookerPrisonerVisitor(bookerReference = booker.reference, prisonerId = prisonerId, linkVisitorRequest, requestReference = requestReference)
    visitorRequestsRepository.approveVisitorRequest(requestReference, LocalDateTime.now())
  }

  private fun getVisitorRequestByReference(requestReference: String): VisitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference) ?: throw VisitorRequestNotFoundException("Request not found for reference $requestReference")
}
