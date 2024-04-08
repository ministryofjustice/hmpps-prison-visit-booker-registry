package uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.helper

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisoner
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisonersVisitor
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AssociatedPrisonerRepository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AssociatedVisitorRepository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AuthDetailRepository

@Component
@Transactional
class EntityHelper(
  val testAuthDetailRepository: AuthDetailRepository,

  val testAssociatedPrisonerRepository: AssociatedPrisonerRepository,

  val testAssociatedVisitorRepository: AssociatedVisitorRepository,
) {

  @Transactional
  fun saveAuthDetail(authDetail: AuthDetail): AuthDetail {
    return testAuthDetailRepository.saveAndFlush(authDetail)
  }

  @Transactional
  fun createAssociatedPrisoner(associatedPrisoner: AssociatedPrisoner): AssociatedPrisoner {
    return testAssociatedPrisonerRepository.saveAndFlush(associatedPrisoner)
  }

  @Transactional
  fun createAssociatedPrisonerVisitor(associatedPrisonersVisitor: AssociatedPrisonersVisitor): AssociatedPrisonersVisitor {
    return testAssociatedVisitorRepository.saveAndFlush(associatedPrisonersVisitor)
  }

  @Transactional
  fun deleteAll() {
    testAssociatedVisitorRepository.deleteAll()
    testAssociatedVisitorRepository.flush()

    testAssociatedPrisonerRepository.deleteAll()
    testAssociatedPrisonerRepository.flush()

    testAuthDetailRepository.deleteAll()
    testAuthDetailRepository.flush()
  }
}
