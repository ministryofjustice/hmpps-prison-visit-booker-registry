package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail

@Repository
interface AuthDetailRepository : JpaRepository<AuthDetail, Long> {

  fun findByOneLoginSub(sub: String): AuthDetail?
}
