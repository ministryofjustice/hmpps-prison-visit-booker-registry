package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisonerVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerVisitorRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@Component
@Transactional
class EntityHelper(
  val testAuthDetailRepository: AuthDetailRepository,

  val testBookerRepository: BookerRepository,

  val testBookerPrisonerRepository: BookerPrisonerRepository,

  val testBookerPrisonerVisitorRepository: BookerPrisonerVisitorRepository,
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
  fun createAssociatedPrisoner(bookerPrisoner: BookerPrisoner): BookerPrisoner {
    return testBookerPrisonerRepository.saveAndFlush(bookerPrisoner)
  }

  @Transactional
  fun createAssociatedPrisonerVisitor(bookerPrisonerVisitor: BookerPrisonerVisitor): BookerPrisonerVisitor {
    return testBookerPrisonerVisitorRepository.saveAndFlush(bookerPrisonerVisitor)
  }

  @Transactional
  fun deleteAll() {
    testBookerPrisonerVisitorRepository.deleteAll()
    testBookerPrisonerVisitorRepository.flush()

    testBookerPrisonerRepository.deleteAll()
    testBookerPrisonerRepository.flush()

    testBookerRepository.deleteAll()
    testBookerRepository.flush()

    testAuthDetailRepository.deleteAll()
    testAuthDetailRepository.flush()
  }
}
