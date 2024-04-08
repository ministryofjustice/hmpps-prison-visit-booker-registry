package uk.gov.justice.digital.hmpps.oneloginuserregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oneloginuserregistry.client.OrchestrationServiceClient
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AssociatedPrisonerDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.PrisonerBasicInfoDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AssociatedPrisoner
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AssociatedPrisonerRepository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AuthDetailRepository

@Service
class PrisonerDetailsService(
  private val authDetailRepository: AuthDetailRepository,
  private val prisonerRepository: AssociatedPrisonerRepository,
  private val orchestrationServiceClient: OrchestrationServiceClient,
) {

  fun getAssociatedPrisoners(reference: String): List<AssociatedPrisonerDto> {
    val authDetailByReference = authDetailRepository.findByAuthReference(reference)
    val associatedPrisoners = mutableListOf<AssociatedPrisonerDto>()
    return authDetailByReference?.let { authDetail ->
      val associatedPrisonersByAuthId = prisonerRepository.findByBookerId(authDetail.id)
      if (associatedPrisonersByAuthId.isNotEmpty()) {
        val prisonerDetails =
          orchestrationServiceClient.getPrisonerDetails(associatedPrisonersByAuthId.map { it.prisonNumber }.toList())?.associateBy { it.prisonerNumber } ?: emptyMap()

        associatedPrisonersByAuthId.forEach {
          associatedPrisoners.add(AssociatedPrisonerDto(prisonerDetails[it.prisonNumber] ?: getBlankPrisonerBasicInfo(it.prisonNumber), it))
        }
      }

      associatedPrisoners
    } ?: emptyList()
  }

  fun getAssociatedPrisoner(reference: String, prisonerId: String): AssociatedPrisoner? {
    val authDetailByReference = authDetailRepository.findByAuthReference(reference)
    return authDetailByReference?.let { authDetail ->
      prisonerRepository.findByAuthDetailIdAndPrisonNumber(authDetail.id, prisonerId)
    }
  }

  private fun getBlankPrisonerBasicInfo(prisonerId: String): PrisonerBasicInfoDto {
    return PrisonerBasicInfoDto(prisonerId, "UNKNOWN", "UNKNOWN", null, null)
  }
}
