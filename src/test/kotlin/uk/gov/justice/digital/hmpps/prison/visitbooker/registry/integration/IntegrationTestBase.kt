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
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
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
    return entityHelper.saveBooker(Booker(oneLoginSub = oneLoginSub, email = emailAddress))
  }
  fun createPrisoner(booker: Booker, prisonerId: String): PermittedPrisoner {
    return entityHelper.createAssociatedPrisoner(PermittedPrisoner(bookerId = booker.id, booker = booker, prisonerId = prisonerId, active = true))
  }

  fun createVisitor(permittedPrisoner: PermittedPrisoner, visitorId: Long): PermittedVisitor {
    return entityHelper.createAssociatedPrisonerVisitor(PermittedVisitor(permittedPrisonerId = permittedPrisoner.id, permittedPrisoner = permittedPrisoner, visitorId = visitorId, active = true))
  }

  fun createAssociatedPrisoners(
    booker: Booker,
    associatedPrisoners: List<PermittedPrisonerTestObject>,
    visitors: List<PermittedVisitorTestObject> = listOf(PermittedVisitorTestObject(1L, true)),
  ): List<PermittedPrisoner> {
    val permittedPrisonerList = mutableListOf<PermittedPrisoner>()
    associatedPrisoners.forEach {
      val permittedPrisoner = createAssociatedPrisoner(PermittedPrisoner(bookerId = booker.id, booker = booker, prisonerId = it.prisonerId, active = it.isActive))
      permittedPrisonerList.add(permittedPrisoner)
      createAssociatedPrisonersVisitors(permittedPrisoner, visitors)
    }
    booker.permittedPrisoners.clear()
    booker.permittedPrisoners.addAll(permittedPrisonerList)

    return permittedPrisonerList
  }

  fun createAssociatedPrisoner(permittedPrisoner: PermittedPrisoner): PermittedPrisoner {
    return entityHelper.createAssociatedPrisoner(permittedPrisoner)
  }

  fun createAssociatedPrisonersVisitors(permittedPrisoner: PermittedPrisoner, associatedPrisonersVisitors: List<PermittedVisitorTestObject>): List<PermittedVisitor> {
    val permittedVisitors = mutableListOf<PermittedVisitor>()
    associatedPrisonersVisitors.forEach {
      val permittedVisitor = PermittedVisitor(permittedPrisonerId = permittedPrisoner.id, permittedPrisoner = permittedPrisoner, visitorId = it.visitorId, active = it.isActive)
      permittedVisitors.add(createAssociatedPrisonersVisitor(permittedPrisoner, permittedVisitor))
    }
    return permittedVisitors
  }

  fun createAssociatedPrisonersVisitor(permittedPrisoner: PermittedPrisoner, permittedVisitor: PermittedVisitor): PermittedVisitor {
    return entityHelper.createAssociatedPrisonerVisitor(permittedVisitor)
  }
}
