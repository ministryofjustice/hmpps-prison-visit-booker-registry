package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisonersVisitor

@Repository
interface AssociatedVisitorRepository : JpaRepository<AssociatedPrisonersVisitor, Long> {
  fun findByAssociatedPrisonerId(associatedPrisonerId: Long): List<AssociatedPrisonersVisitor>
}
