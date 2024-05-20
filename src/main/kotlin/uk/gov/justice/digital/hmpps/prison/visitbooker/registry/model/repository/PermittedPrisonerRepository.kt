package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner

@Repository
interface PermittedPrisonerRepository : JpaRepository<PermittedPrisoner, Long> {
  fun findByBookerId(bookerId: Long): List<PermittedPrisoner>
  fun findByBookerIdAndActive(bookerId: Long, active: Boolean): List<PermittedPrisoner>
  fun findByBookerIdAndPrisonerId(bookerId: Long, prisonerId: String): PermittedPrisoner?
}
