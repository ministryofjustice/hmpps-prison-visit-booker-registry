package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import java.util.function.Supplier

class PublicPrisonsNotFoundException(message: String? = null, cause: Throwable? = null) :
  RuntimeException(message, cause),
  Supplier<PublicPrisonsNotFoundException> {
  override fun get(): PublicPrisonsNotFoundException {
    return PublicPrisonsNotFoundException(message, cause)
  }
}
