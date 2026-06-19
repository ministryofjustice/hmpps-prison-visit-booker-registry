package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.PermittedPrisonerRepository

@Service
class PrisonerMergeService(
  private val permittedPrisonerRepository: PermittedPrisonerRepository,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun mergePrisoner(oldPrisonerNumber: String, newPrisonerNumber: String) {
    LOG.info("Merging prisoner details, updating from old prisoner number - {} to new prisoner number - {}", oldPrisonerNumber, newPrisonerNumber)

    val updatedRecords = permittedPrisonerRepository.mergePrisoner(oldPrisonerNumber, newPrisonerNumber)
    LOG.info("Merging prisoner details, updated {} from old prisoner number - {} to new prisoner number - {}", updatedRecords, oldPrisonerNumber, newPrisonerNumber)
  }
}
