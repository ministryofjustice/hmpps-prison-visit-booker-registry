package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AssociatedPrisonerDto
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
  companion object {
    private const val NOT_KNOWN = "NOT_KNOWN"
  }

  fun getAssociatedPrisoners(reference: String): List<AssociatedPrisonerDto> {
    val bookerByReference = getBooker(reference)
    val associatedPrisoners = mutableListOf<AssociatedPrisonerDto>()

    val associatedPrisonersByAuthId = prisonerRepository.findByBookerId(bookerByReference.id)
    if (associatedPrisonersByAuthId.isNotEmpty()) {
      val prisonerDetails =
        orchestrationServiceClient.getPrisonerDetails(associatedPrisonersByAuthId.map { it.prisonNumber }.toList())?.associateBy { it.prisonerNumber } ?: emptyMap()

      associatedPrisonersByAuthId.forEach {
        associatedPrisoners.add(AssociatedPrisonerDto(prisonerDetails[it.prisonNumber] ?: getBlankPrisonerBasicInfo(it.prisonNumber), it))
      }
    }

    return associatedPrisoners
  }

  fun getAssociatedPrisoner(reference: String, prisonerId: String): BookerPrisoner? {
    val bookerByReference = getBooker(reference)
    return prisonerRepository.findByBookerIdAndPrisonNumber(bookerByReference.id, prisonerId)
  }

  private fun getBlankPrisonerBasicInfo(prisonerId: String): PrisonerBasicInfoDto {
    return PrisonerBasicInfoDto(prisonerId, NOT_KNOWN, NOT_KNOWN)
  }

  private fun getBooker(reference: String): Booker {
    return bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
  }
}
