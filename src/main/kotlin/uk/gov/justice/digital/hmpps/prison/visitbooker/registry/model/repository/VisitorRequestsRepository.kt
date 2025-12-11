package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import java.time.LocalDateTime

@Repository
interface VisitorRequestsRepository : JpaRepository<VisitorRequest, Long> {
  @Query("select vr from VisitorRequest vr where vr.bookerReference = :bookerReference and vr.status = 'REQUESTED'")
  fun findAllActiveRequestsByBookerReference(bookerReference: String): List<VisitorRequest>

  @Query("select count(vr) from VisitorRequest vr inner join PermittedPrisoner pp on pp.prisonerId = vr.prisonerId and pp.prisonCode = :prisonCode where vr.status = 'REQUESTED'")
  fun findCountOfVisitorRequestsByPrisonCode(prisonCode: String): Int

  @Query("select vr from VisitorRequest vr inner join PermittedPrisoner pp on pp.prisonerId = vr.prisonerId and pp.prisonCode = :prisonCode where vr.status = 'REQUESTED'")
  fun findVisitorRequestsByPrisonCode(prisonCode: String): List<VisitorRequest>

  fun findVisitorRequestByReference(reference: String): VisitorRequest?

  @Modifying
  @Query("update VisitorRequest vr set vr.status = 'APPROVED', vr.modifyTimestamp = :modifyTimestamp where vr.reference = :reference")
  fun approveVisitorRequest(reference: String, modifyTimestamp: LocalDateTime)
}
