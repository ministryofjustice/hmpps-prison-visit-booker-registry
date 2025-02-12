package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class BookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerNotFoundException> {
  override fun get(): BookerNotFoundException = BookerNotFoundException(message, cause)
}
