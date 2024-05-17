package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor

@Repository
interface PermittedVisitorRepository : JpaRepository<PermittedVisitor, Long> {
  fun findByPermittedPrisonerId(prisonerPermittedId: Long): List<PermittedVisitor>
  fun findByPermittedPrisonerIdAndActive(prisonerPermittedId: Long, active: Boolean): List<PermittedVisitor>
}
