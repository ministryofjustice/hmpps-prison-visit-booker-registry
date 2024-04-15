package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exceptions

import java.util.function.Supplier

class PrisonerForBookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PrisonerForBookerNotFoundException> {
  override fun get(): PrisonerForBookerNotFoundException {
    return PrisonerForBookerNotFoundException(message, cause)
  }
}
