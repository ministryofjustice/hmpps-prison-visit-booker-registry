package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class VisitorForPrisonerBookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorForPrisonerBookerNotFoundException> {
  override fun get(): VisitorForPrisonerBookerNotFoundException = VisitorForPrisonerBookerNotFoundException(message, cause)
}
