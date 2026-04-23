package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class ContactNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<ContactNotFoundException> {
  override fun get(): ContactNotFoundException = ContactNotFoundException(message, cause)
}
