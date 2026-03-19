package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events

import org.junit.jupiter.api.AfterEach
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
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.helper.EntityHelper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.DomainEventListener
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.DomainEventListener.Companion.PRISON_VISITS_CREATE_CONTACT_EVENT_QUEUE_CONFIG_KEY
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
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  @Autowired
  protected lateinit var entityHelper: EntityHelper

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  internal val topic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  internal val prisonVisitsCreateContactEventQueue by lazy {
    hmppsQueueService.findByQueueId(
      PRISON_VISITS_CREATE_CONTACT_EVENT_QUEUE_CONFIG_KEY,
    ) as HmppsQueue
  }

  internal val domainEventsSqsClient by lazy { prisonVisitsCreateContactEventQueue.sqsClient }
  internal val domainEventsSqsDlqClient by lazy { prisonVisitsCreateContactEventQueue.sqsDlqClient }
  internal val domainEventsQueueUrl by lazy { prisonVisitsCreateContactEventQueue.queueUrl }
  internal val domainEventsDlqUrl by lazy { prisonVisitsCreateContactEventQueue.dlqUrl }

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

  @AfterEach
  @BeforeEach
  fun cleanQueue() {
    purgeQueue(domainEventsSqsClient, domainEventsQueueUrl)
    purgeQueue(domainEventsSqsDlqClient!!, domainEventsDlqUrl!!)
  }

  @BeforeEach
  fun setup() {
    visitorRequestsRepositorySpy.deleteAll()
  }

  @AfterEach
  fun cleanUp() {
    visitorRequestsRepositorySpy.deleteAll()
  }

  fun purgeQueue(client: SqsAsyncClient, url: String) {
    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).get()
  }

  fun createDomainEventPublishRequest(domainEvent: String): PublishRequest? = PublishRequest.builder()
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
}
