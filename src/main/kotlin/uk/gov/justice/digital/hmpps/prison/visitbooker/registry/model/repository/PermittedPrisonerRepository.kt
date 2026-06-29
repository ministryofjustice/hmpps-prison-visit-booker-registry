package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner

@Repository
interface PermittedPrisonerRepository : JpaRepository<PermittedPrisoner, Long> {
  fun findByBookerId(bookerId: Long): List<PermittedPrisoner>

  fun existsByBookerIdAndPrisonerIdIgnoreCase(bookerId: Long, prisonerId: String): Boolean

  @Transactional(readOnly = true)
  @Query(
    "Select pp.* FROM permitted_prisoner pp " +
      "   LEFT JOIN booker b ON b.id = pp.booker_id " +
      " WHERE b.reference = :bookerReference AND lower(prisoner_id) = lower(:prisonerId)",
    nativeQuery = true,
  )
  fun findByBookerIdAndPrisonerId(bookerReference: String, prisonerId: String): PermittedPrisoner?

  @Transactional
  @Modifying
  @Query("UPDATE PermittedPrisoner pp set pp.prisonerId = :newPrisonerId WHERE lower(pp.prisonerId) = lower(:oldPrisonerId)")
  fun mergePrisoner(oldPrisonerId: String, newPrisonerId: String): Int

  @Transactional
  @Modifying
  @Query("UPDATE PermittedPrisoner pp set pp.prisonerId = :newPrisonerId WHERE lower(pp.prisonerId) = lower(:oldPrisonerId) and pp.booker.reference not in (:ignoredBookerReferences)")
  fun mergePrisonerExceptBookers(oldPrisonerId: String, newPrisonerId: String, ignoredBookerReferences: List<String>): Int
}
