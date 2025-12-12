package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class VisitorRequestAlreadyActionedException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorRequestAlreadyActionedException> {
  override fun get(): VisitorRequestAlreadyActionedException = VisitorRequestAlreadyActionedException(message, cause)
}
