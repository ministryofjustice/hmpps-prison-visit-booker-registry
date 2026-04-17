package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate

@DisplayName("Test for Domain Event Prisoner Contact Created")
class DomainEventsPrisonerContactCreatedUpdatedTest : EventsIntegrationTestBase() {
  companion object {
    @JvmStatic
    fun events(): List<String> = listOf(
      DomainEventTypes.PRISONER_CONTACT_CREATED_EVENT.value,
      DomainEventTypes.PRISONER_CONTACT_UPDATED_EVENT.value,
    )
  }

  @ParameterizedTest(name = "when domain event ''{0}'' is found, then it is processed")
  @MethodSource("events")
  fun `when domain event is found, then it is processed`(event: String) {
    // Given
    val prisonerId = "AA123456"
    val contactId = "123456"
    val relationshipId = 9876L
    val contact = PrisonerContactDto(personId = 543L, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21), approvedVisitor = true, contactType = "S")

    val domainEvent = createDomainEventJson(
      event,
      createPrisonerContactCreatedUpdatedEventAdditionalInformationJson(prisonerContactId = relationshipId),
      prisonerId,
      contactId,
    )

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val visitorRequest = createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("john", "smith", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId, contact)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(2)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(2)).handleMessage(any()) }
    await untilAsserted { verify(prisonerContactCreatedUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.AUTO_APPROVED)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
  }

  @ParameterizedTest(name = "when domain event ''{0}'' is found but no visitor requests exist, then it is skipped")
  @MethodSource("events")
  fun `when domain event is found but no visitor requests exist, then it is skipped`(event: String) {
    // Given
    val prisonerId = "AA123456"
    val contactId = "123456"
    val relationshipId = 9876L
    val contact = PrisonerContactDto(personId = 543L, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21), approvedVisitor = true, contactType = "S")

    val domainEvent = createDomainEventJson(
      event,
      createPrisonerContactCreatedUpdatedEventAdditionalInformationJson(prisonerContactId = relationshipId),
      prisonerId,
      contactId,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId, contact)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(prisonerContactCreatedUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
  }

  @ParameterizedTest(name = "when domain event ''{0}'' is found but contact is not Social, then it is skipped")
  @MethodSource("events")
  fun `when domain event is found but contact is not Social, then it is skipped`(event: String) {
    // Given
    val prisonerId = "AA123456"
    val contactId = "123456"
    val relationshipId = 9876L
    val contact = PrisonerContactDto(personId = 543L, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21), approvedVisitor = true, contactType = "O")

    val domainEvent = createDomainEventJson(
      event,
      createPrisonerContactCreatedUpdatedEventAdditionalInformationJson(prisonerContactId = relationshipId),
      prisonerId,
      contactId,
    )

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val visitorRequest = createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("john", "smith", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId, contact)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(prisonerContactCreatedUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.REQUESTED)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
  }

  @ParameterizedTest(name = "when domain event ''{0}'' is found but no requests are in status REQUESTED, then it is skipped")
  @MethodSource("events")
  fun `when domain event is found but no requests are in status REQUESTED, then it is skipped`(event: String) {
    // Given
    val prisonerId = "AA123456"
    val contactId = "123456"
    val relationshipId = 9876L
    val contact = PrisonerContactDto(personId = 543L, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21), approvedVisitor = true, contactType = "S")

    val domainEvent = createDomainEventJson(
      event,
      createPrisonerContactCreatedUpdatedEventAdditionalInformationJson(prisonerContactId = relationshipId),
      prisonerId,
      contactId,
    )

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val visitorRequest = createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("john", "smith", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REJECTED,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId, contact)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(prisonerContactCreatedUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.REJECTED)
    verify(prisonerContactRegistryClientSpy, times(0)).getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
  }

  @ParameterizedTest(name = "when domain event ''{0}'' is found but relationship is not found on prisoner-contact-registry, then it is skipped")
  @MethodSource("events")
  fun `when domain event is found but relationship is not found on prisoner-contact-registry, then it is skipped`(event: String) {
    // Given
    val prisonerId = "AA123456"
    val contactId = "123456"
    val relationshipId = 9876L

    val domainEvent = createDomainEventJson(
      event,
      createPrisonerContactCreatedUpdatedEventAdditionalInformationJson(prisonerContactId = relationshipId),
      prisonerId,
      contactId,
    )

    val booker = createBooker("oneSub", "testEmail@test.com")
    val prisoner = createPrisoner(booker, prisonerId)

    val visitorRequest = createVisitorRequest(
      booker.reference,
      prisoner.prisonerId,
      AddVisitorToBookerPrisonerRequestDto("john", "smith", LocalDate.now().minusYears(21)),
      status = VisitorRequestsStatus.REQUESTED,
    )

    prisonerContactRegistryMockServer.stubGetPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId, null, HttpStatus.NOT_FOUND)

    val publishRequest = createDomainEventPublishRequest(domainEvent)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(domainEventListenerSpy, times(1)).processMessage(any()) }
    await untilAsserted { verify(domainEventListenerServiceSpy, times(1)).handleMessage(any()) }
    await untilAsserted { verify(prisonerContactCreatedUpdatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.REQUESTED)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
  }
}
