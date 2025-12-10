package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class VisitorRequestNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorRequestNotFoundException> {
  override fun get(): VisitorRequestNotFoundException = VisitorRequestNotFoundException(message, cause)
}
