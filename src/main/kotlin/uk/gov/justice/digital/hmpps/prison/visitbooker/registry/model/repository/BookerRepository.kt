package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker

@Repository
interface BookerRepository : JpaRepository<Booker, Long> {

  fun findByOneLoginSub(authReference: String): Booker?

  fun findByReference(reference: String): Booker?

  fun findByEmailIgnoreCase(emailAddress: String): List<Booker>?

  fun findByEmailIgnoreCaseAndOneLoginSub(emailAddress: String, oneLoginSub: String): Booker?

  @Query(
    "SELECT reference FROM booker WHERE id = :id ",
    nativeQuery = true,
  )
  fun findByBookerId(id: Long): String?

  @Modifying
  @Query(
    "update Booker set email = :emailAddress where reference = :reference",
  )
  fun updateBookerEmailAddress(reference: String, emailAddress: String)
}
