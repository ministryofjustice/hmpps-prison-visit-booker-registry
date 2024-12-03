package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class BookerPrisonerAlreadyExistsException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerPrisonerAlreadyExistsException> {
  override fun get(): BookerPrisonerAlreadyExistsException {
    return BookerPrisonerAlreadyExistsException(message, cause)
  }
}
