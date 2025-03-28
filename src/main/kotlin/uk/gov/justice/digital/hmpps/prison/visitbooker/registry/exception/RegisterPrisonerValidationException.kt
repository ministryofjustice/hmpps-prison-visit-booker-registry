package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError
import java.util.function.Supplier

class RegisterPrisonerValidationException(private val errors: List<RegisterPrisonerValidationError>) :
  ValidationException("Failed to validate prisoner being booked."),
  Supplier<RegisterPrisonerValidationException> {
  override fun get(): RegisterPrisonerValidationException = RegisterPrisonerValidationException(errors)
}
