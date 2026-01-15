package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.BookerValidationErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.PrisonerValidationService

const val VALIDATE_PRISONER: String = "/public/booker/{bookerReference}/permitted/prisoners/{prisonerId}/validate"

@RestController
class ValidationController(
  val prisonerValidationService: PrisonerValidationService,
) {

  @PreAuthorize("hasAnyRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(VALIDATE_PRISONER)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Validates a prisoner for whom the booker is about to book a visit",
    description = "Validates a prisoner for whom the booker is about to book a visit",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Validation passed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get permitted visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Prisoner validation failed",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BookerValidationErrorResponse::class))],
      ),
    ],
  )
  fun validatePrisonerBeforeBooking(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @Parameter(
      description = "Prisoner Id for that needs to be validated.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerId: String,
  ) = prisonerValidationService.validatePrisonerBeforeBooking(bookerReference, prisonerId)
}
