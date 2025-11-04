package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor

@Repository
interface PermittedVisitorRepository : JpaRepository<PermittedVisitor, Long> {
  fun findByPermittedPrisonerId(prisonerPermittedId: Long): List<PermittedVisitor>
  fun findByPermittedPrisonerIdAndActive(prisonerPermittedId: Long, active: Boolean): List<PermittedVisitor>

  @Transactional(readOnly = true)
  @Query(
    "Select pv.* FROM permitted_visitor pv " +
      "   LEFT JOIN permitted_prisoner pp ON pp.id = pv.permitted_prisoner_id" +
      "   LEFT JOIN booker b ON b.id = pp.booker_id " +
      " WHERE b.reference = :bookerReference AND prisoner_id=:prisonerId AND pv.visitor_id = :visitorId",
    nativeQuery = true,
  )
  fun findVisitorBy(bookerReference: String, prisonerId: String, visitorId: Long): PermittedVisitor?

  @Modifying
  @Transactional
  @Query(
    """
  DELETE FROM permitted_visitor pv
  USING permitted_prisoner pp
  JOIN booker b ON b.id = pp.booker_id
  WHERE pv.permitted_prisoner_id = pp.id
    AND b.reference = :bookerReference
    AND pp.prisoner_id = :prisonerId
    AND pv.visitor_id = :visitorId
  """,
    nativeQuery = true,
  )
  fun deleteVisitorBy(bookerReference: String, prisonerId: String, visitorId: Long): Int
}
