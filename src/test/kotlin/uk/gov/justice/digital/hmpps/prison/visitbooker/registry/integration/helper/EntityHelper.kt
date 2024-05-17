package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedVisitorRepository

@Component
@Transactional
class EntityHelper(
  val testAuthDetailRepository: AuthDetailRepository,

  val testBookerRepository: BookerRepository,

  val testPermittedPrisonerRepository: PermittedPrisonerRepository,

  val testPermittedVisitorRepository: PermittedVisitorRepository,
) {

  @Transactional
  fun saveAuthDetail(authDetail: AuthDetail): AuthDetail {
    return testAuthDetailRepository.saveAndFlush(authDetail)
  }

  @Transactional
  fun saveBooker(booker: Booker): Booker {
    return testBookerRepository.saveAndFlush(booker)
  }

  @Transactional
  fun createAssociatedPrisoner(permittedPrisoner: PermittedPrisoner): PermittedPrisoner {
    return testPermittedPrisonerRepository.saveAndFlush(permittedPrisoner)
  }

  @Transactional
  fun createAssociatedPrisonerVisitor(permittedVisitor: PermittedVisitor): PermittedVisitor {
    return testPermittedVisitorRepository.saveAndFlush(permittedVisitor)
  }

  @Transactional
  fun deleteAll() {
    testPermittedVisitorRepository.deleteAll()
    testPermittedVisitorRepository.flush()

    testPermittedPrisonerRepository.deleteAll()
    testPermittedPrisonerRepository.flush()

    testBookerRepository.deleteAll()
    testBookerRepository.flush()

    testAuthDetailRepository.deleteAll()
    testAuthDetailRepository.flush()
  }
}
