package uk.gov.justice.digital.hmpps.prison.visitbooker.registry

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT")
class HmppsPrisonVisitBookerRegistryApplication

fun main(args: Array<String>) {
  runApplication<HmppsPrisonVisitBookerRegistryApplication>(*args)
}
