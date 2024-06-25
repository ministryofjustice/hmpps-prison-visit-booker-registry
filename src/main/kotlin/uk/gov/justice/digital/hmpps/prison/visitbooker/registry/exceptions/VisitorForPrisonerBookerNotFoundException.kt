package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions

import java.util.function.Supplier

class VisitorForPrisonerBookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorForPrisonerBookerNotFoundException> {
  override fun get(): VisitorForPrisonerBookerNotFoundException {
    return VisitorForPrisonerBookerNotFoundException(message, cause)
  }
}
