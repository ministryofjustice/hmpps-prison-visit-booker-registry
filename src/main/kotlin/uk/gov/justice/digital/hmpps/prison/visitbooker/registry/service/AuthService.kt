package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.CreateBookerException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity.Booker
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.repository.BookerRepository

@Service
class AuthService(
  private val bookerRepository: BookerRepository,
  private val bookerAuditService: BookerAuditService,
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun authoriseBooker(createBookerAuthDetail: AuthDetailDto): String {
    // find booker by email address and sub
    var booker = bookerRepository.findByEmailIgnoreCaseAndOneLoginSub(createBookerAuthDetail.email, createBookerAuthDetail.oneLoginSub)
    if (booker == null) {
      // if not found check if the booker exists with the different combinations of email and one login sub
      booker = findByDetailedBookerSearch(createBookerAuthDetail)
    }

    return booker.reference
  }

  // TODO: Should we log to application insights if either of the IF statement come true?
  private fun findByDetailedBookerSearch(createBookerAuthDetail: AuthDetailDto): Booker {
    val bookerViaOneLogin = bookerRepository.findByOneLoginSub(createBookerAuthDetail.oneLoginSub)
    if (bookerViaOneLogin != null) {
      LOG.info("Found booker ${bookerViaOneLogin.reference} via one login sub but with different email. Updating email")
      updateBookerEmailAddress(bookerViaOneLogin, createBookerAuthDetail.email)

      return bookerViaOneLogin
    }

    val bookerViaEmail = bookerRepository.findByEmailIgnoreCase(createBookerAuthDetail.email)?.any() ?: false
    if (bookerViaEmail) {
      LOG.warn("Found existing booker(s) via email search ${createBookerAuthDetail.email} but with different one login subs. Creating new booker for email ${createBookerAuthDetail.email}")
    }

    return createBooker(createBookerAuthDetail)
  }

  private fun createBooker(createBookerAuthDetail: AuthDetailDto): Booker {
    LOG.info("Adding new booker for email {}", createBookerAuthDetail.email)
    val booker = try {
      bookerRepository.saveAndFlush(
        Booker(
          oneLoginSub = createBookerAuthDetail.oneLoginSub,
          email = createBookerAuthDetail.email,
        ),
      )
    } catch (e: Exception) {
      throw CreateBookerException("Unable to create booker for email ${createBookerAuthDetail.email}")
    }
    bookerAuditService.auditBookerCreate(bookerReference = booker.reference, email = booker.email, hasSub = true)

    LOG.info("Booker with email ${createBookerAuthDetail.email} successfully created")

    return booker
  }

  private fun updateBookerEmailAddress(
    booker: Booker,
    newRegisteredEmailAddress: String,
  ) {
    LOG.info("Updating booker email from : {} to : {} for booker reference {}", booker.email, newRegisteredEmailAddress, booker.reference)
    bookerRepository.updateBookerEmailAddress(booker.reference, newRegisteredEmailAddress)
    bookerAuditService.auditUpdateBookerEmailAddress(bookerReference = booker.reference, oldEmail = booker.email, newEmail = newRegisteredEmailAddress)
    LOG.info("Successfully updated booker email from : {} to : {}", booker.email, newRegisteredEmailAddress)
  }
}
