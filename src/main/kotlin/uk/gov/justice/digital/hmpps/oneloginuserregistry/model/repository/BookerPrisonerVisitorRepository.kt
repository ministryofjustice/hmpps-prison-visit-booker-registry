package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.BookerPrisonerVisitor

@Repository
interface BookerPrisonerVisitorRepository : JpaRepository<BookerPrisonerVisitor, Long> {
  fun findByBookerPrisonerId(bookerPrisonerId: Long): List<BookerPrisonerVisitor>
}