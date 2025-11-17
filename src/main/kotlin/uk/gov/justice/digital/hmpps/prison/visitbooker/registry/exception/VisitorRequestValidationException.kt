package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestValidationError
import java.util.function.Supplier

class VisitorRequestValidationException(val error: VisitorRequestValidationError) :
  ValidationException("Failed to validate visitor request."),
  Supplier<VisitorRequestValidationException> {
  override fun get(): VisitorRequestValidationException = VisitorRequestValidationException(error)
}
