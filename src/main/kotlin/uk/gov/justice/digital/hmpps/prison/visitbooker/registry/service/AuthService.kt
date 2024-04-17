package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.util.QuotableEncoder

@Service
class AuthService(
  private val authDetailRepository: AuthDetailRepository,
  private val bookerRepository: BookerRepository,
) {

  @Transactional
  fun bookerAuthorisation(createBookerAuthDetailDto: AuthDetailDto): String {
    val authDetail = saveOrGet(createBookerAuthDetailDto)
    return match(authDetail).reference
  }

  private fun match(authDetail: AuthDetail): Booker {
    var booker: Booker
    if (authDetail.count == 0) {
      booker = bookerRepository.findByEmail(authDetail.email) ?: throw BookerNotFoundException("Booker for Email : ${authDetail.email} not found")
      booker.oneLoginSub = authDetail.oneLoginSub
      // Create reference and then save TODO this should be using post persist, we using when we stop manual entry for bookers
      if (booker.reference.isNullOrBlank()) {
        booker.reference = QuotableEncoder(minLength = 10).encode(booker.id)
      }
      booker = bookerRepository.saveAndFlush(booker)
    } else {
      booker = bookerRepository.findByOneLoginSub(authDetail.oneLoginSub) ?: throw BookerNotFoundException("Booker for sub : ${authDetail.oneLoginSub} not found")
    }
    return booker
  }

  private fun saveOrGet(createBookerAuthDetailDto: AuthDetailDto): AuthDetail {
    return authDetailRepository.findByOneLoginSub(createBookerAuthDetailDto.oneLoginSub)?.let {
      it.email = createBookerAuthDetailDto.email
      it.phoneNumber = createBookerAuthDetailDto.phoneNumber
      it.count++
      it
    } ?: run {
      authDetailRepository.saveAndFlush(AuthDetail(createBookerAuthDetailDto))
    }
  }
}
