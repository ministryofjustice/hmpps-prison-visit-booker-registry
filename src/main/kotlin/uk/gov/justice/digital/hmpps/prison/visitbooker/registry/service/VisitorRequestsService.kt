package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorRequestValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.VisitorRequest
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.VisitorRequestsRepository

@Transactional
@Service
class VisitorRequestsService(
  val visitorRequestsRepository: VisitorRequestsRepository,
  val bookerDetailsService: BookerDetailsService,
  val bookerAuditService: BookerAuditService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun submitVisitorRequest(bookerReference: String, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto) {
    LOG.info("Entered VisitorRequestsService - submitVisitorRequest - For booker $bookerReference, prisoner $prisonerId")

    validateVisitorRequest(bookerReference, prisonerId, visitorRequest)

    bookerAuditService.auditVisitorRequest(bookerReference, prisonerId)

    visitorRequestsRepository.save(
      VisitorRequest(
        bookerReference = bookerReference,
        prisonerId = prisonerId,
        firstName = visitorRequest.firstName,
        lastName = visitorRequest.lastName,
        dateOfBirth = visitorRequest.dateOfBirth,
        status = VisitorRequestsStatus.REQUESTED,
      ),
    )
  }

  private fun validateVisitorRequest(bookerReference: String, prisonerId: String, visitorRequest: AddVisitorToBookerPrisonerRequestDto) {
    LOG.info("Entered VisitorRequestsService - validateVisitorRequest - For booker $bookerReference, prisoner $prisonerId")

    val booker = bookerDetailsService.getBookerByReference(bookerReference)
    if (!(booker.permittedPrisoners.any { it.prisonerId == prisonerId })) {
      LOG.error("Booker with reference $bookerReference does not have a permitted prisoner with id $prisonerId")
      throw VisitorRequestValidationException(VisitorRequestValidationError.PRISONER_NOT_FOUND_FOR_BOOKER)
    }

    LOG.info("Successfully validated visitor request - For booker $bookerReference, prisoner $prisonerId")
  }
}
