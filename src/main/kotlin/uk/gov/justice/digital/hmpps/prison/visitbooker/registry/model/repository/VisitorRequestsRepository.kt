package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest

@Repository
interface VisitorRequestsRepository : JpaRepository<VisitorRequest, Long> {
  @Query("select vr from VisitorRequest vr where vr.bookerReference = :bookerReference and vr.status = 'REQUESTED'")
  fun findAllActiveRequestsByBookerReference(bookerReference: String): List<VisitorRequest>

  @Query("select count(vr) from VisitorRequest vr inner join PermittedPrisoner pp on pp.prisonerId = vr.prisonerId and pp.prisonCode = :prisonCode")
  fun findCountOfVisitorRequestsByPrisonCode(prisonCode: String): Int
}
