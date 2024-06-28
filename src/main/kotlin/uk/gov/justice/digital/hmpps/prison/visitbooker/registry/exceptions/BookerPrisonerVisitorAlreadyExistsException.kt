package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions

import java.util.function.Supplier

class BookerPrisonerVisitorAlreadyExistsException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerPrisonerVisitorAlreadyExistsException> {
  override fun get(): BookerPrisonerVisitorAlreadyExistsException {
    return BookerPrisonerVisitorAlreadyExistsException(message, cause)
  }
}
