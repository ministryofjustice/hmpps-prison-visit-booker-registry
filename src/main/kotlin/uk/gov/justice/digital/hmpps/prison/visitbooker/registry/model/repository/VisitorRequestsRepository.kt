package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest

@Repository
interface VisitorRequestsRepository : JpaRepository<VisitorRequest, Long> {
  @Query("select count(*) from VisitorRequest where bookerReference = :bookerReference and status = 'REQUESTED'")
  fun countAllActiveRequestsByBookerReference(bookerReference: String): Int
}
