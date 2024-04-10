package uk.gov.justice.digital.hmpps.oneloginuserregistry.exceptions

import java.util.function.Supplier

class BookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerNotFoundException> {
  override fun get(): BookerNotFoundException {
    return BookerNotFoundException(message, cause)
  }
}
