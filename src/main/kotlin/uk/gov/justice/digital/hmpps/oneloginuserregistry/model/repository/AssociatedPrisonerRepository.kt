package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisoner

@Repository
interface AssociatedPrisonerRepository : JpaRepository<AssociatedPrisoner, Long> {
  fun findByBookerId(bookerId: Long): List<AssociatedPrisoner>
  fun findByAuthDetailIdAndPrisonNumber(authDetailId: Long, prisonNumber: String): AssociatedPrisoner?
}
