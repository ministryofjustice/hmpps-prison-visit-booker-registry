package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.orchestration.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerPrisonerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@Service
class PrisonerDetailsService(
  private val bookerRepository: BookerRepository,
  private val prisonerRepository: BookerPrisonerRepository,
  private val orchestrationServiceClient: OrchestrationServiceClient,
) {
  fun getAssociatedPrisoners(reference: String): List<PrisonerBasicInfoDto> {
    val bookerByReference = getBooker(reference)
    val associatedPrisonersByAuthId = prisonerRepository.findByBookerIdAndActive(bookerByReference.id, true)
    return if (associatedPrisonersByAuthId.isNotEmpty()) {
      orchestrationServiceClient.getPrisonerDetails(associatedPrisonersByAuthId.map { it.prisonNumber }.toList()) ?: emptyList()
    } else {
      emptyList()
    }
  }

  fun getAssociatedPrisoner(reference: String, prisonerId: String): BookerPrisoner? {
    val bookerByReference = getBooker(reference)
    return prisonerRepository.findByBookerIdAndPrisonNumber(bookerByReference.id, prisonerId)
  }

  private fun getBooker(reference: String): Booker {
    return bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
  }
}
