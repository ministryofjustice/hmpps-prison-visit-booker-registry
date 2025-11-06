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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.BookerPrisonerValidationErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerAuditDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ErrorResponseDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.RegisterPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerDetailsService
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.PrisonerValidationService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker/{bookerReference}"
const val PERMITTED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/permitted/prisoners"
const val PERMITTED_VISITORS: String = "$PERMITTED_PRISONERS/{prisonerId}/permitted/visitors"
const val VALIDATE_PRISONER: String = "$PERMITTED_PRISONERS/{prisonerId}/validate"
const val REGISTER_PRISONER: String = "$PERMITTED_PRISONERS/register"
const val GET_BOOKER_AUDIT: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/audit"

@RestController
class BookerDetailsController(
  val bookerDetailsService: BookerDetailsService,
  val prisonerValidationService: PrisonerValidationService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PERMITTED_PRISONERS)
  @Operation(
    summary = "Get permitted prisoners associated with a booker.",
    description = "Get permitted prisoners associated with a booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned permitted prisoners associated with a booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get permitted prisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get permitted prisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPermittedPrisonersForBooker(
    @PathVariable(value = "bookerReference", required = true)
    @Parameter(
      description = "Booker's unique reference.",
      example = "A12345DC",
    )
    @NotBlank
    bookerReference: String,
    @RequestParam(value = "active", required = false)
    @Parameter(
      description = "Returns active / inactive permitted prisoners or returns all permitted prisoners if this parameter is not passed.",
      example = "true",
    )
    active: Boolean?,
  ): List<PermittedPrisonerDto> = bookerDetailsService.getPermittedPrisoners(bookerReference, active)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(PERMITTED_VISITORS)
  @Operation(
    summary = "Get permitted visitors for a permitted prisoner associated with that booker.",
    description = "Get permitted visitors for a permitted prisoner associated with that booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned permitted visitors for a permitted prisoner associated with that booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get permitted visitors for a permitted prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get permitted visitors for a permitted prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPermittedVisitorsForPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @Parameter(
      description = "prisonerId Id for whom permitted visitors need to be returned.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerId: String,
    @RequestParam(value = "active", required = false)
    @Parameter(
      description = "Returns active / inactive permitted visitors for a permitted prisoner or returns all permitted visitors for the permitted prisoner if this parameter is not passed.",
      example = "true",
    )
    active: Boolean?,
  ): List<PermittedVisitorDto> = bookerDetailsService.getPermittedVisitors(bookerReference, prisonerId, active)

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
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BookerPrisonerValidationErrorResponse::class))],
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
  ) {
    prisonerValidationService.validatePrisonerBeforeBooking(bookerReference, prisonerId)
  }

  @PreAuthorize("hasAnyRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @PutMapping(REGISTER_PRISONER)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Validates a prisoner's details against prisoner search and adds the prisoner against the prisoner if valid",
    description = "Validates a prisoner's details against prisoner search and adds the prisoner against the prisoner if valid",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Prisoner added against booker for booking visits.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to register a prisoner against a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Register prisoner validation failed",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun registerPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @RequestBody
    registerPrisonerRequestDto: RegisterPrisonerRequestDto,
  ) = bookerDetailsService.registerPrisoner(bookerReference, registerPrisonerRequestDto)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_BOOKER_AUDIT)
  @Operation(
    summary = "Get a booker's audit information",
    description = "Get a booker's audit information",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a booker's audit information",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get booker's audit information",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get booker's audit information",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The booker does not exist or has no audit history",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun getBookerAudit(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): List<BookerAuditDto> = bookerDetailsService.getBookerAudit(bookerReference)
}
