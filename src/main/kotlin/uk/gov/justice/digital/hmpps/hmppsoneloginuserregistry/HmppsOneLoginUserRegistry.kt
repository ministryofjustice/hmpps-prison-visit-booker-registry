package uk.gov.justice.digital.hmpps.hmppsoneloginuserregistry

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsOneLoginUserRegistry

fun main(args: Array<String>) {
  runApplication<HmppsOneLoginUserRegistry>(*args)
}
