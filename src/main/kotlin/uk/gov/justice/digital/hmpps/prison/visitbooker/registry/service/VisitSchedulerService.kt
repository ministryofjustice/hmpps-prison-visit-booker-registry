package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.client.VisitSchedulerClient

@Service
class VisitSchedulerService(
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  fun getSupportedPublicPrisons(): List<String> {
    return visitSchedulerClient.getSupportedPublicPrisons()
  }
}
