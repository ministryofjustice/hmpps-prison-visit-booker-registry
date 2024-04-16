package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner

@Repository
interface BookerPrisonerRepository : JpaRepository<BookerPrisoner, Long> {
  fun findByBookerIdAndActive(bookerId: Long, active: Boolean): List<BookerPrisoner>
  fun findByBookerIdAndPrisonNumber(bookerId: Long, prisonNumber: String): BookerPrisoner?
}
