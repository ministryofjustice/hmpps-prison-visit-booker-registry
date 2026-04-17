package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.IntegrationTestBase.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.EntityHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.PrisonerContactRegistryMockServer
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.DomainEventListener
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.DomainEventListener.Companion.PRISON_VISITS_BOOKER_EVENTS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.additionalinfo.PrisonerContactCreatedAdditionalInfo
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.handlers.PrisonerContactCreatedEventHandler
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
@AutoConfigureWebTestClient
abstract class EventsIntegrationTestBase {
  companion object {
    internal val prisonerContactRegistryMockServer = PrisonerContactRegistryMockServer()

    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonerContactRegistryMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonerContactRegistryMockServer.stop()
    }
  }

  @Autowired
  protected lateinit var entityHelper: EntityHelper

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  internal val topic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  internal val prisonVisitsBookerEventsQueue by lazy {
    hmppsQueueService.findByQueueId(
      PRISON_VISITS_BOOKER_EVENTS_QUEUE_CONFIG_KEY,
    ) as HmppsQueue
  }

  internal val domainEventsSqsClient by lazy { prisonVisitsBookerEventsQueue.sqsClient }
  internal val domainEventsSqsDlqClient by lazy { prisonVisitsBookerEventsQueue.sqsDlqClient }
  internal val domainEventsQueueUrl by lazy { prisonVisitsBookerEventsQueue.queueUrl }
  internal val domainEventsDlqUrl by lazy { prisonVisitsBookerEventsQueue.dlqUrl }

  internal val awsSnsClient by lazy { topic.snsClient }
  internal val topicArn by lazy { topic.arn }

  @MockitoSpyBean
  protected lateinit var domainEventListenerSpy: DomainEventListener

  @MockitoSpyBean
  protected lateinit var domainEventListenerServiceSpy: DomainEventListenerService

  @MockitoSpyBean
  protected lateinit var prisonerContactCreatedEventHandlerSpy: PrisonerContactCreatedEventHandler

  @MockitoSpyBean
  protected lateinit var visitorRequestsRepositorySpy: VisitorRequestsRepository

  @MockitoSpyBean
  protected lateinit var bookerRepositorySpy: BookerRepository

  @MockitoSpyBean
  protected lateinit var permittedPrisonerRepository: VisitorRequestsRepository

  @MockitoSpyBean
  protected lateinit var permittedVisitorRepository: VisitorRequestsRepository

  @MockitoSpyBean
  protected lateinit var prisonerContactRegistryClientSpy: PrisonerContactRegistryClient

  @BeforeEach
  fun cleanQueue() {
    purgeQueue(domainEventsSqsClient, domainEventsQueueUrl)
    purgeQueue(domainEventsSqsDlqClient!!, domainEventsDlqUrl!!)
  }

  @BeforeEach
  fun setup() {
    bookerRepositorySpy.deleteAll()
    permittedPrisonerRepository.deleteAll()
    permittedVisitorRepository.deleteAll()
    visitorRequestsRepositorySpy.deleteAll()
    prisonerContactRegistryMockServer.resetAll()
  }

  fun purgeQueue(client: SqsAsyncClient, url: String) {
    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).get()
  }

  fun createDomainEventPublishRequest(domainEvent: String): PublishRequest = PublishRequest.builder()
    .topicArn(topicArn)
    .message(domainEvent)
    .build()

  fun createDomainEventJson(
    eventType: String,
    additionalInformation: String,
    nomsId: String,
    contactId: String,
  ): String =
    """
  {
    "eventType":"$eventType",
    "additionalInformation":$additionalInformation,
    "personReference":{
      "identifiers":[
        {
          "type":"NOMS",
          "value":"$nomsId"
        },
        {
          "type":"DPS_CONTACT_ID",
          "value":"$contactId"
        }
      ]
    }
  }
    """.trimIndent().replace("\n", "").replace("  ", "")

  fun createPrisonerContactCreatedEventAdditionalInformationJson(prisonerContactId: Long): String = TestObjectMapper.mapper
    .writeValueAsString(PrisonerContactCreatedAdditionalInfo(prisonerContactId = prisonerContactId))

  fun createBooker(oneLoginSub: String, emailAddress: String): Booker {
    val booker = entityHelper.saveBooker(Booker(oneLoginSub = oneLoginSub, email = emailAddress))
    return entityHelper.saveBooker(booker)
  }
  fun createPrisoner(booker: Booker, prisonerId: String, prisonCode: String = PRISON_CODE): PermittedPrisoner = entityHelper.createAssociatedPrisoner(PermittedPrisoner(bookerId = booker.id, booker = booker, prisonerId = prisonerId, prisonCode = prisonCode))

  fun createVisitorRequest(bookerReference: String, prisonerId: String, addVisitorToBookerPrisonerRequestDto: AddVisitorToBookerPrisonerRequestDto, status: VisitorRequestsStatus): VisitorRequest = entityHelper.createVisitorRequest(
    VisitorRequest(
      bookerReference = bookerReference,
      prisonerId = prisonerId,
      firstName = addVisitorToBookerPrisonerRequestDto.firstName,
      lastName = addVisitorToBookerPrisonerRequestDto.lastName,
      dateOfBirth = addVisitorToBookerPrisonerRequestDto.dateOfBirth,
      status = status,
    ),
  )
}
