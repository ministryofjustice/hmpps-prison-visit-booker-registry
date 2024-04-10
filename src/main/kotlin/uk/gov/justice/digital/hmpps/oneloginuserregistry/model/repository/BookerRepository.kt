package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.Booker

@Repository
interface BookerRepository : JpaRepository<Booker, Long> {

  fun findByOneLoginSub(authReference: String): Booker?

  fun findByEmail(authReference: String): Booker?
}
