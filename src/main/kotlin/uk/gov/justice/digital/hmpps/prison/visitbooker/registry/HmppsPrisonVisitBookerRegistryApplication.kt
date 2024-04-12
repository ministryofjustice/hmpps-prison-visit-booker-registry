package uk.gov.justice.digital.hmpps.prison.visitbooker.registry

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT")
@EnableCaching
class HmppsOneLoginUserRegistryApplication

fun main(args: Array<String>) {
  runApplication<HmppsOneLoginUserRegistryApplication>(*args)
}
