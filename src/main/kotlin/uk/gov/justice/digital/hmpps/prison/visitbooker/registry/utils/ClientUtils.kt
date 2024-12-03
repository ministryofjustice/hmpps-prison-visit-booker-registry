package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils

import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

class ClientUtils {
  companion object {
    fun isNotFoundError(e: Throwable?) =
      e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND
  }
}
