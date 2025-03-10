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
) {
  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun authoriseBooker(createBookerAuthDetail: AuthDetailDto): String {
    // find booker by email address and sub
    var booker = bookerRepository.findByEmailIgnoreCaseAndOneLoginSub(createBookerAuthDetail.email, createBookerAuthDetail.oneLoginSub)

    if (booker == null) {
      // if not found check if the booker exists with the same email address but different sub
      booker = createBookerPostChecks(createBookerAuthDetail)
    }

    return booker?.reference ?: createBooker(createBookerAuthDetail).reference
  }

  private fun createBookerPostChecks(createBookerAuthDetail: AuthDetailDto): Booker? {
    var booker = bookerRepository.findByEmailIgnoreCase(createBookerAuthDetail.email)
    // if not found check if the booker for the same sub but different email address exists
    if (booker == null) {
      booker = bookerRepository.findByOneLoginSub(createBookerAuthDetail.oneLoginSub)?.also {
        processBookerWithMatchingSubButDifferentEmailAddress(createBookerAuthDetail, it)
      }
    } else {
      processBookerWithMatchingEmailAddressButDifferentSub(createBookerAuthDetail, booker).also {
        booker = it
      }
    }

    return booker
  }

  private fun processBookerWithMatchingEmailAddressButDifferentSub(createBookerAuthDetail: AuthDetailDto, booker: Booker): Booker {
    // TODO - set the existing booker to inactive?
    return createBooker(createBookerAuthDetail)
  }

  private fun processBookerWithMatchingSubButDifferentEmailAddress(createBookerAuthDetail: AuthDetailDto, booker: Booker) {
    // if found with a sub - update the email address on the booker entry and return the updated entry
    // TODO - would be ideal to add some audit logging for the booker here
    updateBookerEmailAddress(booker, createBookerAuthDetail.email)
  }

  private fun createBooker(createBookerAuthDetail: AuthDetailDto): Booker {
    LOG.info("Adding new booker for email address {}", createBookerAuthDetail.email)
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

    LOG.info("Booker with email ${createBookerAuthDetail.email} successfully created")

    return booker
  }

  private fun updateBookerEmailAddress(
    booker: Booker,
    newRegisteredEmailAddress: String,
  ) {
    LOG.info("Updating booker email address from : {} to : {}", booker.email, newRegisteredEmailAddress)
    bookerRepository.updateBookerEmailAddress(booker.reference, newRegisteredEmailAddress)
    LOG.info(
      "Successfully updated booker email address from : {} to : {}",
      booker.email,
      newRegisteredEmailAddress,
    )
  }
}
