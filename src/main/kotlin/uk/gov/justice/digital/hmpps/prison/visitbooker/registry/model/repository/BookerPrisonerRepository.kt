package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Prisoner

@Repository
interface BookerPrisonerRepository : JpaRepository<Prisoner, Long> {
  fun findByBookerId(bookerId: Long): List<Prisoner>
  fun findByBookerIdAndActive(bookerId: Long, active: Boolean): List<Prisoner>
  fun findByBookerIdAndPrisonerId(bookerId: Long, prisonerId: String): Prisoner?
}
