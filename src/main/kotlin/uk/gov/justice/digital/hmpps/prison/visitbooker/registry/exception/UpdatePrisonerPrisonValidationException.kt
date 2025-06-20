package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception

import jakarta.validation.ValidationException
import java.util.function.Supplier

class UpdatePrisonerPrisonValidationException(val errorMessage: String) :
  ValidationException(errorMessage),
  Supplier<UpdatePrisonerPrisonValidationException> {
  override fun get(): UpdatePrisonerPrisonValidationException = UpdatePrisonerPrisonValidationException(errorMessage)
}
