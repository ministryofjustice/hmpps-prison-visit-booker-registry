package uk.gov.justice.digital.hmpps.oneloginuserregistry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AuthDetailRepository

@Service
class AuthService(
  private val authDetailRepository: AuthDetailRepository,
) {

  fun save(createBookerAuthDetailDto: AuthDetailDto): AuthDetailDto {
    val existingEntity = authDetailRepository.findByAuthReference(createBookerAuthDetailDto.authReference)
    return existingEntity?.let {
      AuthDetailDto(it)
    } ?: run {
      AuthDetailDto(authDetailRepository.save(AuthDetail(createBookerAuthDetailDto)))
    }
  }
}
