package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository

@Service
class BookerAuditService(
  private val bookerAuditRepository: BookerAuditRepository,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun auditBookerEvent(bookerReference: String, text: String) {
    LOG.debug("Auditing audit booker event for $bookerReference")
    bookerAuditRepository.saveAndFlush(
      BookerAudit(bookerReference = bookerReference, text = text),
    )
  }
}
