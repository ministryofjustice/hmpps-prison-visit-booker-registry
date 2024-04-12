package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.AuthDetail

@Repository
interface AuthDetailRepository : JpaRepository<AuthDetail, Long> {

  fun findByOneLoginSub(sub: String): AuthDetail?
}
