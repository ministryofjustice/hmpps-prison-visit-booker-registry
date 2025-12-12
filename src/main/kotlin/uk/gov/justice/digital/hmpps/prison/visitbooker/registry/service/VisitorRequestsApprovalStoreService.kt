package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import java.time.LocalDateTime

@Service
class VisitorRequestsApprovalStoreService(
  private val visitorRepository: PermittedVisitorRepository,
  private val visitorRequestsRepository: VisitorRequestsRepository,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun approveAndLinkVisitor(bookerPrisoner: PermittedPrisoner, visitorId: Long, requestReference: String) {
    LOG.info("Enter VisitorRequestsApprovalStoreService approveAndLinkVisitor, prisoner Id - ${bookerPrisoner.prisonerId}, visitorId = $visitorId")

    visitorRepository.saveAndFlush(
      PermittedVisitor(
        permittedPrisonerId = bookerPrisoner.id,
        permittedPrisoner = bookerPrisoner,
        visitorId = visitorId,
      ),
    )
    visitorRequestsRepository.approveVisitorRequest(requestReference, LocalDateTime.now())

    LOG.info("Visitor $visitorId successfully linked to prisoner ${bookerPrisoner.prisonerId} for booker ${bookerPrisoner.booker.reference} and request reference $requestReference set to approved")
  }
}
