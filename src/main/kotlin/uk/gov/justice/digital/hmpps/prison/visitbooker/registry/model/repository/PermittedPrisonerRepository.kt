package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner

@Repository
interface PermittedPrisonerRepository : JpaRepository<PermittedPrisoner, Long> {
  fun findByBookerId(bookerId: Long): List<PermittedPrisoner>
  fun findByBookerIdAndActive(bookerId: Long, active: Boolean): List<PermittedPrisoner>

  @Transactional
  @Query(
    "Select pp.* FROM permitted_prisoner pp " +
      "   LEFT JOIN booker b ON b.id = pp.booker_id " +
      " WHERE b.reference = :bookerReference AND prisoner_id=:prisonerId",
    nativeQuery = true,
  )
  fun findByBookerIdAndPrisonerId(bookerReference: String, prisonerId: String): PermittedPrisoner?


  @Transactional
  @Query(
    "Select pp.* FROM permitted_prisoner pp" +
      "   LEFT JOIN booker b ON b.id = pp.booker_id " +
      " WHERE b.referenc = :bookerReference AND prisoner_id=:prisonerId ",
    nativeQuery = true,
  )
  fun getBookerPrisoner(bookerReference: String, prisonerId: String): PermittedPrisoner?
}
