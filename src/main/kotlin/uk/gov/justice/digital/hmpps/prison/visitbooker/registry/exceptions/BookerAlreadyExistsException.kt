package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions

import java.util.function.Supplier

class BookerAlreadyExistsException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerAlreadyExistsException> {
  override fun get(): BookerAlreadyExistsException {
    return BookerAlreadyExistsException(message, cause)
  }
}
