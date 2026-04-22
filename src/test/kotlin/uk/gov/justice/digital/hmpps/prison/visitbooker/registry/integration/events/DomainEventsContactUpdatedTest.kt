package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@DisplayName("Test for Domain Event Contact Updated")
class DomainEventsContactUpdatedTest : EventsIntegrationTestBase() {
  @Test
  fun `when domain event 'contact updated' is found, then it is processed`() {
    // Given
    val contactId = "123456"

    val domainEvent = createDomainEventJson(
      DomainEventTypes.CONTACT_UPDATED_EVENT.value,
      createContactUpdatedEventAdditionalInformationJson(contactId = contactId.toLong()),
      contactId = contactId,
    )

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(contactUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }
  }
}
