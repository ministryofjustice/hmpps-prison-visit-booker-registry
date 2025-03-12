package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CLEAR_BOOKER_CONFIG_CONTROLLER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CREATE_BOOKER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CREATE_BOOKER_PRISONER_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.CREATE_BOOKER_PRISONER_VISITOR_PATH
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.EntityHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.PrisonOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.VisitSchedulerMockServer
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerAudit
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerAuditRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class IntegrationTestBase {
  companion object {
    val objectMapper: ObjectMapper = ObjectMapper().registerModules(JavaTimeModule(), kotlinModule())
    const val PRISON_CODE = "HEI"

    internal val prisonOffenderSearchMockServer = PrisonOffenderSearchMockServer()
    internal val visitSchedulerMockServer = VisitSchedulerMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonOffenderSearchMockServer.start()
      visitSchedulerMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonOffenderSearchMockServer.stop()
      visitSchedulerMockServer.stop()
    }
  }

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var entityHelper: EntityHelper

  @Autowired
  protected lateinit var bookerRepository: BookerRepository

  @Autowired
  protected lateinit var permittedPrisonerRepository: PermittedPrisonerRepository

  @Autowired
  protected lateinit var bookerAuditRepository: BookerAuditRepository

  protected lateinit var orchestrationServiceRoleHttpHeaders: (HttpHeaders) -> Unit
  protected lateinit var bookerConfigServiceRoleHttpHeaders: (HttpHeaders) -> Unit

  @BeforeEach
  internal fun setUpRoles() {
    orchestrationServiceRoleHttpHeaders =
      setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE"))
    bookerConfigServiceRoleHttpHeaders =
      setAuthorisation(roles = listOf("ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG"))
  }

  @BeforeEach
  fun resetStubs() {
    prisonOffenderSearchMockServer.resetAll()
    visitSchedulerMockServer.resetAll()
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
    return entityHelper.saveBooker(booker)
  }
  fun createPrisoner(booker: Booker, prisonerId: String): PermittedPrisoner = entityHelper.createAssociatedPrisoner(PermittedPrisoner(bookerId = booker.id, booker = booker, prisonerId = prisonerId, active = true, prisonCode = PRISON_CODE))

  fun createVisitor(permittedPrisoner: PermittedPrisoner, visitorId: Long): PermittedVisitor = entityHelper.createAssociatedPrisonerVisitor(PermittedVisitor(permittedPrisonerId = permittedPrisoner.id, permittedPrisoner = permittedPrisoner, visitorId = visitorId, active = true))

  fun createAssociatedPrisoners(
    booker: Booker,
    associatedPrisoners: List<PermittedPrisonerTestObject>,
    visitors: List<PermittedVisitorTestObject> = listOf(PermittedVisitorTestObject(1L, true)),
  ): List<PermittedPrisoner> {
    val permittedPrisonerList = mutableListOf<PermittedPrisoner>()
    associatedPrisoners.forEach {
      val permittedPrisoner = createAssociatedPrisoner(PermittedPrisoner(bookerId = booker.id, booker = booker, prisonerId = it.prisonerId, active = it.isActive, prisonCode = PRISON_CODE))
      permittedPrisonerList.add(permittedPrisoner)
      createAssociatedPrisonersVisitors(permittedPrisoner, visitors)
    }
    booker.permittedPrisoners.clear()
    booker.permittedPrisoners.addAll(permittedPrisonerList)

    return permittedPrisonerList
  }

  fun createAssociatedPrisoner(permittedPrisoner: PermittedPrisoner): PermittedPrisoner = entityHelper.createAssociatedPrisoner(permittedPrisoner)

  fun createAssociatedPrisonersVisitors(permittedPrisoner: PermittedPrisoner, associatedPrisonersVisitors: List<PermittedVisitorTestObject>): List<PermittedVisitor> {
    val permittedVisitors = mutableListOf<PermittedVisitor>()
    associatedPrisonersVisitors.forEach {
      val permittedVisitor = PermittedVisitor(permittedPrisonerId = permittedPrisoner.id, permittedPrisoner = permittedPrisoner, visitorId = it.visitorId, active = it.isActive)
      permittedVisitors.add(createAssociatedPrisonersVisitor(permittedPrisoner, permittedVisitor))
    }
    return permittedVisitors
  }

  fun createAssociatedPrisonersVisitor(permittedPrisoner: PermittedPrisoner, permittedVisitor: PermittedVisitor): PermittedVisitor = entityHelper.createAssociatedPrisonerVisitor(permittedVisitor)

  protected fun callCreateBooker(
    authHttpHeaders: (HttpHeaders) -> Unit,
    createBookerDto: CreateBookerDto,
  ): ResponseSpec = webTestClient.put().uri(CREATE_BOOKER_PATH)
    .headers(authHttpHeaders)
    .body(BodyInserters.fromValue(createBookerDto))
    .exchange()

  protected fun callCreateBookerPrisoner(
    authHttpHeaders: (HttpHeaders) -> Unit,
    createPermittedPrisonerDto: CreatePermittedPrisonerDto,
    bookerReference: String,
  ): ResponseSpec {
    val uri = CREATE_BOOKER_PRISONER_PATH.replace("{bookerReference}", bookerReference)
    return webTestClient.put().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(createPermittedPrisonerDto))
      .exchange()
  }

  protected fun callCreateBookerPrisonerVisitor(
    authHttpHeaders: (HttpHeaders) -> Unit,
    createPermittedVisitorDto: CreatePermittedVisitorDto,
    bookerReference: String,
    prisonerId: String,
  ): ResponseSpec {
    val uri = CREATE_BOOKER_PRISONER_VISITOR_PATH.replace("{bookerReference}", bookerReference)
      .replace("{prisonerId}", prisonerId)

    return webTestClient.put().uri(uri)
      .headers(authHttpHeaders)
      .body(BodyInserters.fromValue(createPermittedVisitorDto))
      .exchange()
  }

  protected fun assertError(responseSpec: ResponseSpec, userMessage: String, developerMessage: String, status: HttpStatus) {
    responseSpec
      .expectStatus().isEqualTo(status)
      .expectBody()
      .jsonPath("$.userMessage").value(Matchers.equalTo(userMessage))
      .jsonPath("$.developerMessage").value(Matchers.containsString(developerMessage))
  }

  protected fun callClearBookerDetails(
    authHttpHeaders: (HttpHeaders) -> Unit,
    bookerReference: String,
  ): ResponseSpec = webTestClient.delete().uri(CLEAR_BOOKER_CONFIG_CONTROLLER_PATH.replace("{bookerReference}", bookerReference))
    .headers(authHttpHeaders)
    .exchange()

  protected fun getBookerDto(responseSpec: ResponseSpec): BookerDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, BookerDto::class.java)

  protected fun getPermittedPrisonerDto(responseSpec: ResponseSpec): PermittedPrisonerDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, PermittedPrisonerDto::class.java)

  protected fun getPermittedVisitorDto(responseSpec: ResponseSpec): PermittedVisitorDto = objectMapper.readValue(responseSpec.expectBody().returnResult().responseBody, PermittedVisitorDto::class.java)

  protected fun assertAuditEvent(auditEvent: BookerAudit, bookerReference: String, auditType: BookerAuditType, text: String) {
    assertThat(auditEvent.bookerReference).isEqualTo(bookerReference)
    assertThat(auditEvent.auditType).isEqualTo(auditType)
    assertThat(auditEvent.text).isEqualTo(text)
  }
}
