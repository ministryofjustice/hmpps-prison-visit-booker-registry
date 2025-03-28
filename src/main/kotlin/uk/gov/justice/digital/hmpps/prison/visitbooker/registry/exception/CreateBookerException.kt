package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class CreateBookerException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<CreateBookerException> {
  override fun get(): CreateBookerException = CreateBookerException(message, cause)
}
