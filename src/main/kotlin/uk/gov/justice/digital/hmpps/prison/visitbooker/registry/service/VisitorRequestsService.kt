package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository

@Transactional
@Service
class VisitorRequestsService(
  val visitorRequestsRepository: VisitorRequestsRepository,
  val bookerAuditService: BookerAuditService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun submitVisitorRequest(bookerReference: String, prisonerId: String, visitorRequest: AddVisitorToBookerRequestDto) {
    LOG.info("Entered VisitorRequestsService - submitVisitorRequest - For booker $bookerReference, prisoner $prisonerId")

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
}
