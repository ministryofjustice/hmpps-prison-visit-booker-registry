package uk.gov.justice.digital.hmpps.oneloginuserregistry.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import reactor.util.function.Tuple2
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.helper.EntityHelper
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.mock.VisitsOrchestrationMockServer
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisoner
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisonersVisitor
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {
  companion object {
    val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())
    val visitsOrchestrationMockServer = VisitsOrchestrationMockServer(objectMapper)

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitsOrchestrationMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitsOrchestrationMockServer.stop()
    }
  }

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var entityHelper: EntityHelper

  @AfterEach
  fun deleteAll() {
    entityHelper.deleteAll()
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun createAuthDetail(authDetailDto: AuthDetailDto): AuthDetail {
    val authDetail = AuthDetail(authReference = authDetailDto.authReference, authEmail = authDetailDto.authEmail, authPhoneNumber = authDetailDto.authPhoneNumber)
    return entityHelper.saveAuthDetail(authDetail)
  }

  fun createAssociatedPrisoners(authDetail: AuthDetail, associatedPrisoners: List<Tuple2<String, Boolean>>): List<AssociatedPrisoner> {
    val associatedPrisonerList = mutableListOf<AssociatedPrisoner>()
    associatedPrisoners.forEach {
      val associatedPrisoner = AssociatedPrisoner(bookerId = authDetail.id, authDetail = authDetail, prisonNumber = it.t1, active = it.t2)
      associatedPrisonerList.add(createAssociatedPrisoner(authDetail, associatedPrisoner))
    }
    return associatedPrisonerList
  }

  @Transactional
  fun createAssociatedPrisoner(authDetail: AuthDetail, associatedPrisoner: AssociatedPrisoner): AssociatedPrisoner {
    return entityHelper.createAssociatedPrisoner(associatedPrisoner)
  }

  fun createAssociatedPrisonersVisitors(associatedPrisoner: AssociatedPrisoner, associatedPrisonersVisitors: List<Tuple2<Long, Boolean>>): List<AssociatedPrisonersVisitor> {
    val visitors = mutableListOf<AssociatedPrisonersVisitor>()
    associatedPrisonersVisitors.forEach {
      val associatedPrisonersVisitor = AssociatedPrisonersVisitor(associatedPrisonerId = associatedPrisoner.id, associatedPrisoner = associatedPrisoner, visitorId = it.t1, active = it.t2)
      visitors.add(createAssociatedPrisonersVisitor(associatedPrisoner, associatedPrisonersVisitor))
    }

    return visitors
  }

  @Transactional
  fun createAssociatedPrisonersVisitor(associatedPrisoner: AssociatedPrisoner, associatedPrisonersVisitor: AssociatedPrisonersVisitor): AssociatedPrisonersVisitor {
    return entityHelper.createAssociatedPrisonerVisitor(associatedPrisonersVisitor)
  }
}
