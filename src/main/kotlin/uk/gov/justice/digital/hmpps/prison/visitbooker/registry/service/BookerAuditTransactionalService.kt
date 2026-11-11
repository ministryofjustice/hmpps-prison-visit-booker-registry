package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditAttributeTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAuditAttribute
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository

@Service
@Transactional
class BookerAuditTransactionalService(
  private val bookerAuditRepository: BookerAuditRepository,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(readOnly = true)
  fun getBookerAudit(bookerReference: String): List<BookerAudit> = bookerAuditRepository.findByBookerReference(bookerReference)

  fun saveBookerAudit(bookerReference: String, auditType: BookerAuditType, text: String, attributes: Map<BookerAuditAttributeTypes, String>) {
    LOG.debug("Auditing audit booker event for $bookerReference")

    val bookerAudit = bookerAuditRepository.save(
      BookerAudit(bookerReference = bookerReference, auditType = auditType, text = text),
    )

    attributes.forEach { (bookerAuditAttributeType, value) ->
      bookerAudit.bookerAuditAttributes.add(
        BookerAuditAttribute(
          bookerAuditId = bookerAudit.id,
          attributeName = bookerAuditAttributeType,
          attributeValue = value,
          bookerAudit = bookerAudit,
        ),
      )
    }

    bookerAuditRepository.saveAndFlush(bookerAudit)
  }
}
