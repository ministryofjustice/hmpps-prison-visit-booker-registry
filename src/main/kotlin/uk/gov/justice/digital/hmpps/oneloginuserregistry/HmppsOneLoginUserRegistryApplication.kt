package uk.gov.justice.digital.hmpps.oneloginuserregistry

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsOneLoginUserRegistryApplication

fun main(args: Array<String>) {
  runApplication<HmppsOneLoginUserRegistryApplication>(*args)
}
