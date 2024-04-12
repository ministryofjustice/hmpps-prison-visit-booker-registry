package uk.gov.justice.digital.hmpps.oneloginuserregistry.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.exceptions.BookerNotFoundException
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.AuthDetail
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity.Booker
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.AuthDetailRepository
import uk.gov.justice.digital.hmpps.oneloginuserregistry.model.repository.BookerRepository

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
      // Save to add reference
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