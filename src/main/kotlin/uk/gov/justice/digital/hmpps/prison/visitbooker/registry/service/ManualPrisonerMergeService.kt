package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonerMergeRequestDto

@Service
class ManualPrisonerMergeService(
  private val prisonerMergeService: PrisonerMergeService,
  private val telemetryClientService: TelemetryClientService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MANUAL_MERGE_EVENT_FAILED_FOR_PRISONER = "manual_prisoner_merge_event_failed"
  }

  fun mergePrisoner(oldPrisonerNumber: String, newPrisonerNumber: String) {
    prisonerMergeService.mergePrisoner(oldPrisonerNumber, newPrisonerNumber)
  }

  fun mergePrisoners(prisonerMerges: List<PrisonerMergeRequestDto>) {
    prisonerMerges.forEach {
      mergePrisonerAndLogFailure(
        oldPrisonerNumber = it.oldPrisonerNumber,
        newPrisonerNumber = it.newPrisonerNumber,
      )
    }
  }

  private fun mergePrisonerAndLogFailure(oldPrisonerNumber: String, newPrisonerNumber: String) {
    try {
      prisonerMergeService.mergePrisoner(oldPrisonerNumber, newPrisonerNumber)
    } catch (e: Exception) {
      LOG.error("Failed to manually merge prisoner number from {} to {}", oldPrisonerNumber, newPrisonerNumber, e)
      telemetryClientService.trackEvent(
        MANUAL_MERGE_EVENT_FAILED_FOR_PRISONER,
        mapOf(
          "oldPrisonerNumber" to oldPrisonerNumber,
          "newPrisonerNumber" to newPrisonerNumber,
          "exceptionType" to e::class.java.name,
          "exceptionMessage" to (e.message ?: ""),
        ),
      )
    }
  }
}
