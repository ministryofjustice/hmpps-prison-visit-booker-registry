package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.EntityHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Prisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Visitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {
  companion object {
    val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())
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

  protected lateinit var orchestrationServiceRoleHttpHeaders: (HttpHeaders) -> Unit
  protected lateinit var bookerConfigServiceRoleHttpHeaders: (HttpHeaders) -> Unit

  @BeforeEach
  internal fun setUpRoles() {
    orchestrationServiceRoleHttpHeaders =
      setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE"))
    bookerConfigServiceRoleHttpHeaders =
      setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG"))
  }

  @AfterEach
  fun deleteAll() {
    entityHelper.deleteAll()
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun createBooker(oneLoginSub: String, emailAddress: String): Booker {
    val booker = entityHelper.saveBooker(Booker(oneLoginSub = oneLoginSub, email = emailAddress))
    return booker
  }
  fun createPrisoner(booker: Booker, prisonerId: String): Prisoner {
    return entityHelper.createAssociatedPrisoner(Prisoner(bookerId = booker.id, booker = booker, prisonerId = prisonerId, active = true))
  }

  fun createVisitor(prisoner: Prisoner, visitorId: Long): Visitor {
    return entityHelper.createAssociatedPrisonerVisitor(Visitor(prisonerId = prisoner.id, prisoner = prisoner, visitorId = visitorId, active = true))
  }

  fun createAssociatedPrisoners(
    booker: Booker,
    associatedPrisoners: List<PrisonerDetails>,
    visitors: List<PrisonersVisitorDetails> = listOf(PrisonersVisitorDetails(1L, true)),
  ): List<Prisoner> {
    val prisonerList = mutableListOf<Prisoner>()
    associatedPrisoners.forEach {
      val prisoner = createAssociatedPrisoner(Prisoner(bookerId = booker.id, booker = booker, prisonerId = it.prisonerId, active = it.isActive))
      prisonerList.add(prisoner)
      createAssociatedPrisonersVisitors(prisoner, visitors)
    }
    return prisonerList
  }

  fun createAssociatedPrisoner(prisoner: Prisoner): Prisoner {
    return entityHelper.createAssociatedPrisoner(prisoner)
  }

  fun createAssociatedPrisonersVisitors(prisoner: Prisoner, associatedPrisonersVisitors: List<PrisonersVisitorDetails>): List<Visitor> {
    val visitors = mutableListOf<Visitor>()
    associatedPrisonersVisitors.forEach {
      val visitor = Visitor(prisonerId = prisoner.id, prisoner = prisoner, visitorId = it.visitorId, active = it.isActive)
      visitors.add(createAssociatedPrisonersVisitor(prisoner, visitor))
    }
    return visitors
  }

  fun createAssociatedPrisonersVisitor(prisoner: Prisoner, visitor: Visitor): Visitor {
    return entityHelper.createAssociatedPrisonerVisitor(visitor)
  }
}
