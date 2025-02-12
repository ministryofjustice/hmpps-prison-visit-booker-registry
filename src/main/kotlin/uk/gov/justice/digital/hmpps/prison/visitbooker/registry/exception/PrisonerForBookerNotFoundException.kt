package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class PrisonerForBookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PrisonerForBookerNotFoundException> {
  override fun get(): PrisonerForBookerNotFoundException = PrisonerForBookerNotFoundException(message, cause)
}
