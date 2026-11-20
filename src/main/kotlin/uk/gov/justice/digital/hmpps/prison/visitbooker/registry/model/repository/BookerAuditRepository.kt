package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit

@Repository
interface BookerAuditRepository : JpaRepository<BookerAudit, Long> {
  @Query("select ba from BookerAudit ba left join fetch ba.bookerAuditAttributes where ba.bookerReference = :bookerReference order by ba.createTimestamp")
  fun findByBookerReference(bookerReference: String): List<BookerAudit>

  @Query("select ba from BookerAudit ba left join fetch ba.bookerAuditAttributes order by ba.createTimestamp")
  override fun findAll(): List<BookerAudit>
}
