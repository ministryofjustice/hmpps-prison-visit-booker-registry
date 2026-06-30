package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.IntegrationTestBase.Companion.PRISON_CODE
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.PermittedPrisonerTestObject
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.PermittedVisitor
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import kotlin.random.Random

@DisplayName("Test for Domain Event Prisoner Merged Event")
class DomainEventsPrisonerMergeTest : EventsIntegrationTestBase() {

  private lateinit var booker1: Booker
  private lateinit var booker2: Booker
  private lateinit var booker3: Booker

  @BeforeEach
  internal fun setUp() {
    booker1 = createBooker(oneLoginSub = "123", emailAddress = "test@example.com")
    booker2 = createBooker(oneLoginSub = "456", emailAddress = "test1@example.com")
    booker3 = createBooker(oneLoginSub = "789", emailAddress = "test2@example.com")
  }

  @Test
  fun `when prisoner merge event received then old prisoner number is updated with new prisoner number`() {
    // Given
    val oldPrisonerNumber = "AB123XYZ"
    val newPrisonerNumber = "BB123ABC"
    val otherPrisonerNumber = "OTH123"

    createAssociatedPrisoners(
      booker1,
      listOf(
        PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE),
        PermittedPrisonerTestObject(otherPrisonerNumber, PRISON_CODE),
      ),
    )

    createAssociatedPrisoners(
      booker2,
      listOf(
        PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE),
      ),
    )

    createAssociatedPrisoners(
      booker3,
      listOf(
        PermittedPrisonerTestObject(otherPrisonerNumber, PRISON_CODE),
      ),
    )

    val domainEvent = createPrisonerMergeDomainEventJson(
      eventType = DomainEventTypes.PRISONER_MERGE_EVENT.value,
      oldPrisonerNumber = oldPrisonerNumber,
      newPrisonerNumber = newPrisonerNumber,
    )
    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(prisonerMergeEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    val bookerPrisoners = permittedPrisonerRepository.findAll()
    val booker1Prisoners = bookerPrisoners.filter { it.bookerId == booker1.id }
    assertThat(booker1Prisoners.size).isEqualTo(2)
    assertThat(booker1Prisoners.map { it.prisonerId }).containsAll(listOf(newPrisonerNumber, otherPrisonerNumber))

    val booker2Prisoners = bookerPrisoners.filter { it.bookerId == booker2.id }
    assertThat(booker2Prisoners.size).isEqualTo(1)
    assertThat(booker2Prisoners.map { it.prisonerId }).containsOnly(newPrisonerNumber)

    val booker3Prisoners = bookerPrisoners.filter { it.bookerId == booker3.id }
    assertThat(booker3Prisoners.size).isEqualTo(1)
    assertThat(booker3Prisoners.map { it.prisonerId }).containsOnly(otherPrisonerNumber)
  }

  @Test
  fun `when prisoner merge event received and booker has both old prisoner number and new prisoner number then that booker merge is skipped`() {
    // Given
    val oldPrisonerNumber = "AB123XYZ"
    val newPrisonerNumber = "BB123ABC"
    val otherPrisonerNumber = "OTH123"

    // booker1 already has got both old and new prisoner numbers - should delete the old one
    createAssociatedPrisoners(
      booker1,
      listOf(
        PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE),
        PermittedPrisonerTestObject(newPrisonerNumber, PRISON_CODE),
      ),
    )

    // booker2 has got only old prisoner number
    createAssociatedPrisoners(
      booker2,
      listOf(
        PermittedPrisonerTestObject(oldPrisonerNumber, PRISON_CODE),
      ),
    )

    // booker3 has got only new and other prisoner number
    createAssociatedPrisoners(
      booker3,
      listOf(
        PermittedPrisonerTestObject(newPrisonerNumber, PRISON_CODE),
        PermittedPrisonerTestObject(otherPrisonerNumber, PRISON_CODE),
      ),
    )

    val domainEvent = createPrisonerMergeDomainEventJson(
      eventType = DomainEventTypes.PRISONER_MERGE_EVENT.value,
      oldPrisonerNumber = oldPrisonerNumber,
      newPrisonerNumber = newPrisonerNumber,
    )
    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(prisonerMergeEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    val bookerPrisoners = permittedPrisonerRepository.findAll()
    val booker1Prisoners = bookerPrisoners.filter { it.bookerId == booker1.id }
    assertThat(booker1Prisoners.size).isEqualTo(1)

    // booker1 should have only new prisoner number
    assertThat(booker1Prisoners.map { it.prisonerId }).containsOnly(newPrisonerNumber)

    val booker2Prisoners = bookerPrisoners.filter { it.bookerId == booker2.id }
    assertThat(booker2Prisoners.size).isEqualTo(1)

    // booker2 will have only new prisoner number
    assertThat(booker2Prisoners.map { it.prisonerId }).containsOnly(newPrisonerNumber)

    val booker3Prisoners = bookerPrisoners.filter { it.bookerId == booker3.id }
    assertThat(booker3Prisoners.size).isEqualTo(2)

    // booker3 will have the new and other prisoner number
    assertThat(booker3Prisoners.map { it.prisonerId }).containsAll(listOf(newPrisonerNumber, otherPrisonerNumber))

    // a booker_merge_event_failed should be sent to telemetry for booker1
    verify(telemetryClientSpy, times(1)).trackEvent(
      "booker_merge_event_failed",
      mapOf(
        "bookerReference" to booker1.reference,
        "oldPrisonerNumber" to oldPrisonerNumber,
        "newPrisonerNumber" to newPrisonerNumber,
      ),
      null,
    )
  }

  private fun createAssociatedPrisoners(
    booker: Booker,
    associatedPrisoners: List<PermittedPrisonerTestObject>,
  ): List<PermittedPrisoner> {
    val permittedPrisonerList = mutableListOf<PermittedPrisoner>()

    associatedPrisoners.forEach {
      val permittedPrisoner = createAssociatedPrisoner(PermittedPrisoner(bookerId = booker.id, booker = booker, prisonerId = it.prisonerId, prisonCode = PRISON_CODE))
      permittedPrisoner.permittedVisitors.add(createAssociatedPrisonersVisitor(permittedPrisoner))
      permittedPrisonerList.add(permittedPrisoner)
    }
    booker.permittedPrisoners.clear()
    booker.permittedPrisoners.addAll(permittedPrisonerList)

    return permittedPrisonerList
  }

  private fun createAssociatedPrisoner(permittedPrisoner: PermittedPrisoner): PermittedPrisoner = entityHelper.createAssociatedPrisoner(permittedPrisoner)

  private fun createAssociatedPrisonersVisitor(permittedPrisoner: PermittedPrisoner): PermittedVisitor = entityHelper.createAssociatedPrisonerVisitor(
    PermittedVisitor(
      permittedPrisonerId = permittedPrisoner.id,
      permittedPrisoner = permittedPrisoner,
      visitorId = Random.nextInt(1, 1000).toLong(),
    ),
  )

  private fun createPrisonerMergeDomainEventJson(
    eventType: String,
    oldPrisonerNumber: String,
    newPrisonerNumber: String,
  ): String {
    val additionalInformation = mapOf(
      "removedNomsNumber" to oldPrisonerNumber,
      "nomsNumber" to newPrisonerNumber,
    )

    return createDomainEventJson(eventType = eventType, additionalInformation = TestObjectMapper.mapper.writeValueAsString(additionalInformation), nomsId = newPrisonerNumber)
  }
}
