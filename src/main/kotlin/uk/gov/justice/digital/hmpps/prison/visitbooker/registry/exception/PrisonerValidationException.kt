package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.PrisonerValidationError
import java.util.function.Supplier

class PrisonerValidationException(val error: PrisonerValidationError) :
  ValidationException("Failed to validate prisoner being booked."),
  Supplier<PrisonerValidationException> {
  override fun get(): PrisonerValidationException = PrisonerValidationException(error)
}
