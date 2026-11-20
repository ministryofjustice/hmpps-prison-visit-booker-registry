package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerAuditDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAuditAttribute
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository

@Component
@Transactional
class EntityHelper(
  val testBookerRepository: BookerRepository,
  val testPermittedPrisonerRepository: PermittedPrisonerRepository,
  val testPermittedVisitorRepository: PermittedVisitorRepository,
  val testBookerAuditRepository: BookerAuditRepository,
  val testVisitorRequestsRepository: VisitorRequestsRepository,
) {
  @Transactional
  fun saveBooker(booker: Booker): Booker = testBookerRepository.saveAndFlush(booker)

  @Transactional
  fun createAssociatedPrisoner(permittedPrisoner: PermittedPrisoner): PermittedPrisoner = testPermittedPrisonerRepository.saveAndFlush(permittedPrisoner)

  @Transactional
  fun createVisitorRequest(visitorRequest: VisitorRequest): VisitorRequest = testVisitorRequestsRepository.saveAndFlush(visitorRequest)

  @Transactional
  fun createAssociatedPrisonerVisitor(permittedVisitor: PermittedVisitor): PermittedVisitor = testPermittedVisitorRepository.saveAndFlush(permittedVisitor)

  @Transactional
  fun createBookerAudit(bookerAuditDto: BookerAuditDto): BookerAudit {
    val bookerAudit = testBookerAuditRepository.save(
      BookerAudit(bookerReference = bookerAuditDto.bookerReference, auditType = bookerAuditDto.auditType, text = bookerAuditDto.text),
    )
    bookerAuditDto.auditAttributes.forEach { bookerAuditAttribute ->
      bookerAudit.bookerAuditAttributes.add(
        BookerAuditAttribute(
          bookerAudit = bookerAudit,
          bookerAuditId = bookerAudit.id,
          attributeName = bookerAuditAttribute.attributeName,
          attributeValue = bookerAuditAttribute.attributeValue,
        ),
      )
    }

    return testBookerAuditRepository.saveAndFlush(bookerAudit)
  }

  @Transactional
  fun deleteAll() {
    testPermittedVisitorRepository.deleteAll()
    testPermittedVisitorRepository.flush()

    testPermittedPrisonerRepository.deleteAll()
    testPermittedPrisonerRepository.flush()

    testBookerRepository.deleteAll()
    testBookerRepository.flush()

    testBookerAuditRepository.deleteAll()
    testBookerAuditRepository.flush()

    testVisitorRequestsRepository.deleteAll()
    testVisitorRequestsRepository.flush()
  }
}
