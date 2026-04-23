package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.ContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.ContactLinkedSocialPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate

@DisplayName("Test for Domain Event Contact Updated")
class DomainEventsContactUpdatedTest : EventsIntegrationTestBase() {
  @Test
  fun `when domain event 'contact updated' is found, then it is processed`() {
    // Given
    val contactId = "123456"
    val prisonerId = "AA123456"

    val domainEvent = createDomainEventJson(
      DomainEventTypes.CONTACT_UPDATED_EVENT.value,
      createContactUpdatedEventAdditionalInformationJson(contactId = contactId.toLong()),
      contactId = contactId,
    )

    val contact = ContactDto(
      contactId = contactId.toLong(),
      firstName = "john",
      lastName = "smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    prisonerContactRegistryMockServer.stubGetContact(contactId, contact)
    prisonerContactRegistryMockServer.stubGetContactLinkedSocialPrisoners(contactId, listOf(ContactLinkedSocialPrisonerDto(prisonerId)))

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val visitorRequest = createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("john", "smith", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(2)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(2)).handleMessage(any()) }
    await untilAsserted { verify(contactUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.AUTO_APPROVED)

    verify(prisonerContactRegistryClientSpy, times(1)).getContact(contactId)
    verify(prisonerContactRegistryClientSpy, times(1)).getContactLinkedSocialPrisoners(contactId)
  }

  @Test
  fun `when domain event 'contact updated' is found but no visitor requests exist, then it is skipped`() {
    // Given
    val contactId = "123456"
    val prisonerId = "AA123456"

    val domainEvent = createDomainEventJson(
      DomainEventTypes.CONTACT_UPDATED_EVENT.value,
      createContactUpdatedEventAdditionalInformationJson(contactId = contactId.toLong()),
      contactId = contactId,
    )

    val contact = ContactDto(
      contactId = contactId.toLong(),
      firstName = "john",
      lastName = "smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    prisonerContactRegistryMockServer.stubGetContact(contactId, contact)
    prisonerContactRegistryMockServer.stubGetContactLinkedSocialPrisoners(contactId, listOf(ContactLinkedSocialPrisonerDto(prisonerId)))

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(contactUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findAll().size).isEqualTo(0)

    verify(prisonerContactRegistryClientSpy, times(1)).getContact(contactId)
    verify(prisonerContactRegistryClientSpy, times(1)).getContactLinkedSocialPrisoners(contactId)
  }

  @Test
  fun `when domain event 'contact updated' is found but no linked social prisoners, then it is skipped`() {
    // Given
    val contactId = "123456"
    val prisonerId = "AA123456"

    val domainEvent = createDomainEventJson(
      DomainEventTypes.CONTACT_UPDATED_EVENT.value,
      createContactUpdatedEventAdditionalInformationJson(contactId = contactId.toLong()),
      contactId = contactId,
    )

    val contact = ContactDto(
      contactId = contactId.toLong(),
      firstName = "john",
      lastName = "smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    prisonerContactRegistryMockServer.stubGetContact(contactId, contact)
    prisonerContactRegistryMockServer.stubGetContactLinkedSocialPrisoners(contactId, emptyList())

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(contactUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findAll().size).isEqualTo(0)

    verify(prisonerContactRegistryClientSpy, times(1)).getContact(contactId)
    verify(prisonerContactRegistryClientSpy, times(1)).getContactLinkedSocialPrisoners(contactId)
  }

  @Test
  fun `when domain event 'contact updated' is found but not found exception on contact-registry returned, then NOT_FOUND is returned`() {
    // Given
    val contactId = "123456"
    val prisonerId = "AA123456"

    val domainEvent = createDomainEventJson(
      DomainEventTypes.CONTACT_UPDATED_EVENT.value,
      createContactUpdatedEventAdditionalInformationJson(contactId = contactId.toLong()),
      contactId = contactId,
    )

    val contact = ContactDto(
      contactId = contactId.toLong(),
      firstName = "john",
      lastName = "smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    prisonerContactRegistryMockServer.stubGetContact(contactId, contact)
    prisonerContactRegistryMockServer.stubGetContactLinkedSocialPrisoners(contactId, null, HttpStatus.NOT_FOUND)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(contactUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    awaitVisitsDlqHasOneMessage()
  }

  @Test
  fun `when domain event 'contact updated' is found but no requests are in status REQUESTED, then it is skipped`() {
    // Given
    val contactId = "123456"
    val prisonerId = "AA123456"

    val domainEvent = createDomainEventJson(
      DomainEventTypes.CONTACT_UPDATED_EVENT.value,
      createContactUpdatedEventAdditionalInformationJson(contactId = contactId.toLong()),
      contactId = contactId,
    )

    val contact = ContactDto(
      contactId = contactId.toLong(),
      firstName = "john",
      lastName = "smith",
      dateOfBirth = LocalDate.now().minusYears(21),
    )

    prisonerContactRegistryMockServer.stubGetContact(contactId, contact)
    prisonerContactRegistryMockServer.stubGetContactLinkedSocialPrisoners(contactId, listOf(ContactLinkedSocialPrisonerDto(prisonerId)))

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val visitorRequest = createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("john", "smith", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REJECTED,
    )

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(contactUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.REJECTED)

    verify(prisonerContactRegistryClientSpy, times(1)).getContact(contactId)
    verify(prisonerContactRegistryClientSpy, times(1)).getContactLinkedSocialPrisoners(contactId)
  }
}
