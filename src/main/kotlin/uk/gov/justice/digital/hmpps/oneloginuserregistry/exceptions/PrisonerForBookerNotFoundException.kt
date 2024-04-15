package uk.gov.justice.digital.hmpps.oneloginuserregistry.exceptions

import java.util.function.Supplier

class PrisonerForBookerNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PrisonerForBookerNotFoundException> {
  override fun get(): PrisonerForBookerNotFoundException {
    return PrisonerForBookerNotFoundException(message, cause)
  }
}
