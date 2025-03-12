package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditType

@Service
class TelemetryClientService(private val telemetryClient: TelemetryClient) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun trackEvent(auditEventType: BookerAuditType, properties: Map<String, String>) {
    try {
      telemetryClient.trackEvent(auditEventType.telemetryEventName, properties, null)
    } catch (e: RuntimeException) {
      LOG.error("Error occurred in call to telemetry client to log event - $e.toString()")
    }
  }
}
