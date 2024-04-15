package uk.gov.justice.digital.hmpps.oneloginuserregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oneloginuserregistry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AssociatedPrisonerDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.orchestration.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.BookerPrisoner
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.BookerPrisonerRepository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.BookerRepository

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
    val bookerByReference = bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
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
    val bookerByReference = bookerRepository.findByReference(reference) ?: throw BookerNotFoundException("Booker for reference : $reference not found")
    return prisonerRepository.findByBookerIdAndPrisonNumber(bookerByReference.id, prisonerId)
  }

  private fun getBlankPrisonerBasicInfo(prisonerId: String): PrisonerBasicInfoDto {
    return PrisonerBasicInfoDto(prisonerId, NOT_KNOWN, NOT_KNOWN)
  }
}
