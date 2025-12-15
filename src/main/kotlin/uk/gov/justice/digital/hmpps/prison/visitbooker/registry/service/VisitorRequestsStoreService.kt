package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
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
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun approveAndLinkVisitor(bookerReference: String, prisonerId: String, visitorId: Long, requestReference: String): PrisonVisitorRequestDto {
    LOG.info("Enter VisitorRequestsApprovalStoreService approveAndLinkVisitor, booker reference - $bookerReference, prisonerId - $prisonerId, visitorId = $visitorId")
    val booker = bookerRepository.findByReference(bookerReference) ?: throw BookerNotFoundException("Booker with reference $bookerReference not found")
    val bookerPrisoner = booker.permittedPrisoners.firstOrNull { it.prisonerId == prisonerId } ?: throw PrisonerNotFoundException("Booker with reference $bookerReference does not have a permitted prisoner with id $prisonerId")

    visitorRepository.saveAndFlush(
      PermittedVisitor(
        permittedPrisonerId = bookerPrisoner.id,
        permittedPrisoner = bookerPrisoner,
        visitorId = visitorId,
      ),
    )
    visitorRequestsRepository.approveVisitorRequest(requestReference, LocalDateTime.now())
    val visitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference) ?: throw VisitorRequestNotFoundException("Request not found for reference $requestReference")
    LOG.info("Visitor $visitorId successfully linked to prisoner ${bookerPrisoner.prisonerId} for booker ${bookerPrisoner.booker.reference} and request reference $requestReference set to approved")
    return PrisonVisitorRequestDto(visitorRequest, booker.email)
  }

  @Transactional(readOnly = true)
  fun getVisitorRequestByReference(requestReference: String): VisitorRequest = visitorRequestsRepository.findVisitorRequestByReference(requestReference) ?: throw VisitorRequestNotFoundException("Request not found for reference $requestReference")

  @Transactional
  fun deleteVisitorRequestsByBookerPrisonerInStatusRequested(bookerReference: String, prisonerId: String) {
    visitorRequestsRepository.deleteVisitorRequestsByBookerReferenceAndPrisonerIdInStatusRequested(bookerReference, prisonerId)
  }
}
