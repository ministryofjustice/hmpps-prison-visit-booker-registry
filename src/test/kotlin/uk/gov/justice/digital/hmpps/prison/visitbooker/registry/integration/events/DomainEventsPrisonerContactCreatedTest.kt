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
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.contact.registry.PrisonerContactDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums.DomainEventTypes
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate

@DisplayName("Test for Domain Event Prisoner Contact Created")
class DomainEventsPrisonerContactCreatedTest : EventsIntegrationTestBase() {
  @Test
  fun `when domain event 'prisoner contact created' is found, then it is processed`() {
    // Given
    val prisonerId = "AA123456"
    val contactId = "123456"
    val relationshipId = 9876L
    val contact = PrisonerContactDto(personId = 543L, firstName = "John", lastName = "Smith", dateOfBirth = LocalDate.now().minusYears(21), approvedVisitor = true, contactType = "S")

    val domainEvent = createDomainEventJson(
      DomainEventTypes.PRISONER_CONTACT_CREATED_EVENT.value,
      createPrisonerContactCreatedEventAdditionalInformationJson(prisonerContactId = relationshipId),
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
    await untilAsserted { verify(prisonerContactCreatedEventHandlerSpy, times(1)).handle(any()) }
    await untilCallTo { domainEventsSqsClient.countMessagesOnQueue(domainEventsQueueUrl).get() } matches { it == 0 }

    assertThat(visitorRequestsRepositorySpy.findVisitorRequestByReference(visitorRequest.reference)!!.status).isEqualTo(VisitorRequestsStatus.APPROVED)
    verify(prisonerContactRegistryClientSpy, times(1)).getPrisonerContactViaRelationshipId(prisonerId, contactId, relationshipId)
  }
}
