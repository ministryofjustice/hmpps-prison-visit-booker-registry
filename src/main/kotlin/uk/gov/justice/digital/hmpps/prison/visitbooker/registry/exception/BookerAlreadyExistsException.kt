package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class BookerAlreadyExistsException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerAlreadyExistsException> {
  override fun get(): BookerAlreadyExistsException = BookerAlreadyExistsException(message, cause)
}
