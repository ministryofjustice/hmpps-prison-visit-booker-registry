package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class PrisonerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PrisonerNotFoundException> {
  override fun get(): PrisonerNotFoundException = PrisonerNotFoundException(message, cause)
}
