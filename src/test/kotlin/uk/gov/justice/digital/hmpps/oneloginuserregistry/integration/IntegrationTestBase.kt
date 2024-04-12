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
import reactor.util.function.Tuple2
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.helper.EntityHelper
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.mock.VisitsOrchestrationMockServer
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.Booker
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.BookerPrisonerVisitor
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.BookerRepository

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

  @Autowired
  protected lateinit var authDetailRepository: AuthDetailRepository

  @Autowired
  protected lateinit var bookerRepository: BookerRepository

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
    val authDetail = AuthDetail(count = 0, oneLoginSub = authDetailDto.oneLoginSub, email = authDetailDto.email, phoneNumber = authDetailDto.phoneNumber)
    return entityHelper.saveAuthDetail(authDetail)
  }

  fun createBooker(oneLoginSub: String, emailAddress: String): Booker {
    val booker = Booker(oneLoginSub = oneLoginSub, email = emailAddress)
    return entityHelper.saveBooker(booker)
  }

  fun createAssociatedPrisoners(booker: Booker, associatedPrisoners: List<Tuple2<String, Boolean>>): List<BookerPrisoner> {
    val bookerPrisonerList = mutableListOf<BookerPrisoner>()
    associatedPrisoners.forEach {
      val bookerPrisoner = BookerPrisoner(bookerId = booker.id, booker = booker, prisonNumber = it.t1, active = it.t2)
      bookerPrisonerList.add(createAssociatedPrisoner(bookerPrisoner))
    }
    return bookerPrisonerList
  }

  fun createAssociatedPrisoner(bookerPrisoner: BookerPrisoner): BookerPrisoner {
    return entityHelper.createAssociatedPrisoner(bookerPrisoner)
  }

  fun createAssociatedPrisonersVisitors(bookerPrisoner: BookerPrisoner, associatedPrisonersVisitors: List<Tuple2<Long, Boolean>>): List<BookerPrisonerVisitor> {
    val visitors = mutableListOf<BookerPrisonerVisitor>()
    associatedPrisonersVisitors.forEach {
      val bookerPrisonerVisitor = BookerPrisonerVisitor(bookerPrisonerId = bookerPrisoner.id, bookerPrisoner = bookerPrisoner, visitorId = it.t1, active = it.t2)
      visitors.add(createAssociatedPrisonersVisitor(bookerPrisoner, bookerPrisonerVisitor))
    }

    return visitors
  }

  fun createAssociatedPrisonersVisitor(bookerPrisoner: BookerPrisoner, bookerPrisonerVisitor: BookerPrisonerVisitor): BookerPrisonerVisitor {
    return entityHelper.createAssociatedPrisonerVisitor(bookerPrisonerVisitor)
  }
}
