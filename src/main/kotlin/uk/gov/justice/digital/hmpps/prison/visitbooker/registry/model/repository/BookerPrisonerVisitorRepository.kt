package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Visitor

@Repository
interface BookerPrisonerVisitorRepository : JpaRepository<Visitor, Long> {
  fun findByPrisonerId(prisonerId: Long): List<Visitor>
  fun findByPrisonerIdAndActive(prisonerId: Long, active: Boolean): List<Visitor>
}
