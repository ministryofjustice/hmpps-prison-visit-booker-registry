package uk.gov.justice.digital.hmpps.prison.visitbooker.registry

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.RegisterPrisonerValidationError
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.prisoner.search.PrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils.RegisterPrisonerValidator
import java.time.LocalDate

@DisplayName("Test for prisoner validation errors")
class RegisterPrisonerValidatorTest {
  private val registerPrisonerValidator = RegisterPrisonerValidator()
  private val prisonerNumber = "A1234567"
  private val prisonCode = "HEI"
  private val firstName = "FirstName"
  private val lastName = "LastName"
  private val dateOfBirth = LocalDate.of(1901, 1, 1)

  @Test
  fun `when prisoner details on register prisoner request and prisoner search are same validation passes an empty error list is returned`() {
    val prisonerDto = PrisonerDto(
      prisonerNumber = prisonerNumber,
      prisonId = prisonCode,
      inOutStatus = null,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
    )

    val registerPrisonerRequestDto = RegisterPrisonerRequestDto(
      prisonerId = prisonerNumber,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
      prisonCode = prisonCode,
    )

    Assertions.assertThat(registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisonerDto)).isEmpty()
  }

  @Test
  fun `when first and last name and prison code on register request and prisoner search is same but in different cases validation passes and an empty error list is returned`() {
    val prisonerDto = PrisonerDto(
      prisonerNumber = prisonerNumber,
      prisonId = prisonCode.lowercase(),
      inOutStatus = null,
      firstName = firstName.lowercase(),
      lastName = lastName.lowercase(),
      dateOfBirth = dateOfBirth,
    )

    val registerPrisonerRequestDto = RegisterPrisonerRequestDto(
      prisonerId = prisonerNumber,
      prisonerFirstName = firstName.uppercase(),
      prisonerLastName = lastName.uppercase(),
      prisonerDateOfBirth = dateOfBirth,
      prisonCode = prisonCode.uppercase(),
    )

    Assertions.assertThat(registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisonerDto)).isEmpty()
  }

  @Test
  fun `when first and last name and prison code on register request and prisoner search is same but not trimmed validation passes and an empty error list is returned`() {
    val prisonerDto = PrisonerDto(
      prisonerNumber = prisonerNumber,
      prisonId = "   $prisonCode   ",
      inOutStatus = null,
      firstName = "   $firstName   ",
      lastName = "   $lastName   ",
      dateOfBirth = dateOfBirth,
    )

    val registerPrisonerRequestDto = RegisterPrisonerRequestDto(
      prisonerId = prisonerNumber,
      prisonerFirstName = firstName,
      prisonerLastName = lastName,
      prisonerDateOfBirth = dateOfBirth,
      prisonCode = prisonCode,
    )

    Assertions.assertThat(registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisonerDto)).isEmpty()
  }

  @Test
  fun `when details on register request and prisoner search do not match validation fails and an error list is returned`() {
    val prisonerDto = PrisonerDto(
      prisonerNumber = prisonerNumber,
      prisonId = prisonCode,
      inOutStatus = null,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
    )

    val registerPrisonerRequestDto = RegisterPrisonerRequestDto(
      prisonerId = prisonerNumber,
      prisonerFirstName = "Test",
      prisonerLastName = "Test",
      prisonerDateOfBirth = LocalDate.of(2001, 1, 1),
      prisonCode = "TST",
    )

    val errors = registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisonerDto)
    Assertions.assertThat(errors).isNotEmpty()
    Assertions.assertThat(errors.size).isEqualTo(4)
    Assertions.assertThat(errors).containsExactlyInAnyOrder(
      RegisterPrisonerValidationError.FIRST_NAME_INCORRECT,
      RegisterPrisonerValidationError.LAST_NAME_INCORRECT,
      RegisterPrisonerValidationError.DOB_INCORRECT,
      RegisterPrisonerValidationError.PRISON_CODE_INCORRECT,
    )
  }

  @Test
  fun `when prisoner search returns nulls validation fails and an error list is returned`() {
    val prisonerDto = PrisonerDto(
      prisonerNumber = prisonerNumber,
      prisonId = null,
      inOutStatus = null,
      firstName = null,
      lastName = null,
      dateOfBirth = null,
    )

    val registerPrisonerRequestDto = RegisterPrisonerRequestDto(
      prisonerId = prisonerNumber,
      prisonerFirstName = "Test",
      prisonerLastName = "Test",
      prisonerDateOfBirth = LocalDate.of(2001, 1, 1),
      prisonCode = "TST",
    )

    val errors = registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisonerDto)
    Assertions.assertThat(errors).isNotEmpty()
    Assertions.assertThat(errors.size).isEqualTo(4)
    Assertions.assertThat(errors).containsExactlyInAnyOrder(
      RegisterPrisonerValidationError.FIRST_NAME_INCORRECT,
      RegisterPrisonerValidationError.LAST_NAME_INCORRECT,
      RegisterPrisonerValidationError.DOB_INCORRECT,
      RegisterPrisonerValidationError.PRISON_CODE_INCORRECT,
    )
  }

  @Test
  fun `when prisoner search does not return a prisoner validation fails and an error list is returned`() {
    val prisonerDto = null

    val registerPrisonerRequestDto = RegisterPrisonerRequestDto(
      prisonerId = prisonerNumber,
      prisonerFirstName = "Test",
      prisonerLastName = "Test",
      prisonerDateOfBirth = LocalDate.of(2001, 1, 1),
      prisonCode = "TST",
    )

    val errors = registerPrisonerValidator.validateAgainstPrisonerSearch(registerPrisonerRequestDto, prisonerDto)
    Assertions.assertThat(errors).isNotEmpty()
    Assertions.assertThat(errors.size).isEqualTo(1)
    Assertions.assertThat(errors).contains(
      RegisterPrisonerValidationError.PRISONER_NOT_FOUND,
    )
  }
}
