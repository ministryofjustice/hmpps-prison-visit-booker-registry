package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class VisitorRequestAlreadyApprovedException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<VisitorRequestAlreadyApprovedException> {
  override fun get(): VisitorRequestAlreadyApprovedException = VisitorRequestAlreadyApprovedException(message, cause)
}
