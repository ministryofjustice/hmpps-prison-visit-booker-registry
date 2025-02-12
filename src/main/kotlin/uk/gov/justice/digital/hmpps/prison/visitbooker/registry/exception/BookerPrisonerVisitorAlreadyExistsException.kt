package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class BookerPrisonerVisitorAlreadyExistsException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<BookerPrisonerVisitorAlreadyExistsException> {
  override fun get(): BookerPrisonerVisitorAlreadyExistsException = BookerPrisonerVisitorAlreadyExistsException(message, cause)
}
