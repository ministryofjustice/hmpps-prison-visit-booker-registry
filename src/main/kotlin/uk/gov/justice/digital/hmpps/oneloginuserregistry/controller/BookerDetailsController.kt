package uk.gov.justice.digital.hmpps.oneloginuserregistry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.oneloginuserregistry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AssociatedPrisonerDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AssociatedPrisonersVisitorDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.service.PrisonerDetailsService
import uk.gov.justice.digital.hmpps.oneloginuserregistry.service.VisitorDetailsService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker/{bookerReference}"

const val BOOKER_LINKED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/prisoners"
const val BOOKER_LINKED_PRISONER_VISITORS: String = "$BOOKER_LINKED_PRISONERS/{prisonerNumber}/visitors"

@RestController
class OrchestrationPublicBookerController(
  val prisonerDetailsService: PrisonerDetailsService,
  val visitorDetailsService: VisitorDetailsService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI')")
  @GetMapping(BOOKER_LINKED_PRISONERS)
  @Operation(
    summary = "Get prisoners associated with a booker.",
    description = "Get prisoners associated with a booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned prisoners associated with a booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get prisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get prisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonersForBooker(
    @PathVariable(value = "bookerReference", required = true)
    @Parameter(
      description = "Booker's unique reference.",
      example = "A12345DC",
    )
    @NotBlank
    bookerReference: String,
  ): List<AssociatedPrisonerDto> {
    return prisonerDetailsService.getAssociatedPrisoners(bookerReference)
  }

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI')")
  @GetMapping(BOOKER_LINKED_PRISONER_VISITORS)
  @Operation(
    summary = "Get visitors for a prisoner associated with that booker.",
    description = "Get visitors for a prisoner associated with that booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned visitors for a prisoner associated with that booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get visitors for a prisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorsForPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerNumber", required = true)
    @Parameter(
      description = "Prisoner Number for whom visitors need to be returned.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerNumber: String,
  ): List<AssociatedPrisonersVisitorDto> {
    return visitorDetailsService.getAssociatedVisitors(bookerReference, prisonerNumber)
  }
}
