package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class VisitorRequestAlreadyRejectedException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorRequestAlreadyRejectedException> {
  override fun get(): VisitorRequestAlreadyRejectedException = VisitorRequestAlreadyRejectedException(message, cause)
}
