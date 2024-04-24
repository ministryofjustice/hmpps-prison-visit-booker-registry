package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisonerVisitor

@Repository
interface BookerPrisonerVisitorRepository : JpaRepository<BookerPrisonerVisitor, Long> {
  fun findByBookerPrisonerId(bookerPrisonerId: Long): List<BookerPrisonerVisitor>
  fun findByBookerPrisonerIdAndActive(bookerPrisonerId: Long, active: Boolean): List<BookerPrisonerVisitor>
}
