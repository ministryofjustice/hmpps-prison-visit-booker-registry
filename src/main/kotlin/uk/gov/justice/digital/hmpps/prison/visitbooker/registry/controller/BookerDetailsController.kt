package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerDetailsService

const val PUBLIC_BOOKER_CONTROLLER_PATH: String = "/public/booker/{bookerReference}"

const val BOOKER_LINKED_PRISONERS: String = "$PUBLIC_BOOKER_CONTROLLER_PATH/permitted/prisoners"
const val BOOKER_LINKED_PRISONER_VISITORS: String = "$BOOKER_LINKED_PRISONERS/{prisonerId}/permitted/visitors"

@RestController
class BookerDetailsController(
  val bookerDetailsService: BookerDetailsService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(BOOKER_LINKED_PRISONERS)
  @Operation(
    summary = "Get permittedPrisoners associated with a booker.",
    description = "Get permittedPrisoners associated with a booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned permittedPrisoners associated with a booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get permittedPrisoners associated with a booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get permittedPrisoners associated with a booker",
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
    @RequestParam(value = "active", required = false)
    @Parameter(
      description = "Returns active / inactive permittedPrisoners or returns all permittedPrisoners if this parameter is not passed.",
      example = "true",
    )
    active: Boolean?,
  ): List<PermittedPrisonerDto> {
    return bookerDetailsService.getAssociatedPrisoners(bookerReference, active)
  }

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(BOOKER_LINKED_PRISONER_VISITORS)
  @Operation(
    summary = "Get permittedVisitors for a permittedPrisoner associated with that booker.",
    description = "Get permittedVisitors for a permittedPrisoner associated with that booker.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returned permittedVisitors for a permittedPrisoner associated with that booker",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get permittedVisitors for a permittedPrisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get permittedVisitors for a permittedPrisoner associated with that booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorsForPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @Parameter(
      description = "prisonerId Id for whom permittedVisitors need to be returned.",
      example = "A12345DC",
    )
    @NotBlank
    prisonerId: String,
    @RequestParam(value = "active", required = false)
    @Parameter(
      description = "Returns active / inactive permittedVisitors for a permittedPrisoner or returns all permittedVisitors for the permittedPrisoner if this parameter is not passed.",
      example = "true",
    )
    active: Boolean?,
  ): List<PermittedVisitorDto> {
    return bookerDetailsService.getAssociatedVisitors(bookerReference, prisonerId, active)
  }
}
