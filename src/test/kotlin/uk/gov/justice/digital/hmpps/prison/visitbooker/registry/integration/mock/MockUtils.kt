package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.integration.mock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.TestObjectMapper

class MockUtils {
  companion object {
    fun getJsonString(obj: Any): String = TestObjectMapper.mapper.writer().writeValueAsString(obj)

    fun createJsonResponseBuilder(): ResponseDefinitionBuilder = WireMock.aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
  }
}
