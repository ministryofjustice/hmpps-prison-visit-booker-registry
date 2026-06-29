package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@Service
class PrisonerMergeService(
  private val permittedPrisonerRepository: PermittedPrisonerRepository,
  private val bookerRepository: BookerRepository,
  private val telemetryClientService: TelemetryClientService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MERGE_EVENT_FAILED_FOR_BOOKER = "booker_merge_event_failed"
  }

  @Transactional
  fun mergePrisoner(oldPrisonerNumber: String, newPrisonerNumber: String) {
    LOG.info("Merging prisoner details, updating from old prisoner number - {} to new prisoner number - {}", oldPrisonerNumber, newPrisonerNumber)

    // check if new prisoner number and old prisoner number both exist for a booker
    val bookersWithBothOldAndNewPrisonerNumber = bookerRepository.findBookersWithBothPrisoners(oldPrisonerNumber, newPrisonerNumber)

    val updatedRecords = if (bookersWithBothOldAndNewPrisonerNumber.isEmpty()) {
      permittedPrisonerRepository.mergePrisoner(oldPrisonerId = oldPrisonerNumber, newPrisonerId = newPrisonerNumber)
    } else {
      val updated = permittedPrisonerRepository.mergePrisonerExceptBookers(oldPrisonerId = oldPrisonerNumber, newPrisonerId = newPrisonerNumber, ignoredBookerReferences = bookersWithBothOldAndNewPrisonerNumber.map { it })

      // add an event for bookers who have been ignored due to both old and new prisoner number being associated
      bookersWithBothOldAndNewPrisonerNumber.forEach { bookerReference ->
        logBookerMergeFailure(bookerReference = bookerReference, oldPrisonerNumber = oldPrisonerNumber, newPrisonerNumber = newPrisonerNumber)
      }

      updated
    }
    LOG.info("Merging prisoner details, updated {} prisoner records from old prisoner number - {} to new prisoner number - {}", updatedRecords, oldPrisonerNumber, newPrisonerNumber)
  }

  private fun logBookerMergeFailure(bookerReference: String, oldPrisonerNumber: String, newPrisonerNumber: String) {
    LOG.error("Booker with reference - {} has both new prisoner number - {} and old prisoner number - {} already associated, skipping prisoner number update", bookerReference, newPrisonerNumber, oldPrisonerNumber)
    telemetryClientService.trackEvent(
      MERGE_EVENT_FAILED_FOR_BOOKER,
      mapOf(
        "bookerReference" to bookerReference,
        "oldPrisonerNumber" to oldPrisonerNumber,
        "newPrisonerNumber" to newPrisonerNumber,
      ),
    )
  }
}
