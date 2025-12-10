package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository

@Transactional
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
        status = VisitorRequestsStatus.REQUESTED,
      ),
    )
  }

  fun getActiveVisitorRequests(bookerReference: String): List<BookerPrisonerVisitorRequestDto> {
    LOG.info("Entered VisitorRequestsService - getAwaitingVisitorRequests - For booker $bookerReference")
    val booker = bookerDetailsService.getBookerByReference(bookerReference)
    return visitorRequestsRepository.findAllActiveRequestsByBookerReference(booker.reference).map { BookerPrisonerVisitorRequestDto(it) }
  }

  fun getVisitorRequestsCountByPrisonCode(prisonCode: String): VisitorRequestsCountByPrisonCodeDto {
    LOG.info("Entered VisitorRequestsService - getVisitorRequestsCountByPrisonCode - For prison $prisonCode")
    return VisitorRequestsCountByPrisonCodeDto(visitorRequestsRepository.findCountOfVisitorRequestsByPrisonCode(prisonCode))
  }

  fun getVisitorRequestsByPrisonCode(prisonCode: String): List<PrisonVisitorRequestDto> {
    LOG.info("Entered VisitorRequestsService - getVisitorRequestsByPrisonCode - For prison $prisonCode")
    val visitorRequests = visitorRequestsRepository.findVisitorRequestsByPrisonCode(prisonCode)

    return visitorRequests.map { visitorRequest ->
      val booker = bookerDetailsService.getBookerByReference(visitorRequest.bookerReference)
      PrisonVisitorRequestDto(visitorRequest, booker.email)
    }.toList()
  }

  fun getVisitorRequest(requestReference: String): PrisonVisitorRequestDto {
    LOG.info("Entered VisitorRequestsService - getVisitorRequest - requestReference $requestReference")

    val request = visitorRequestsRepository.findVisitorRequestByReference(requestReference)
      ?: throw VisitorRequestNotFoundException("Request not found for reference $requestReference")

    val booker = bookerDetailsService.getBookerByReference(request.bookerReference)

    return PrisonVisitorRequestDto(request, booker.email)
  }
}
