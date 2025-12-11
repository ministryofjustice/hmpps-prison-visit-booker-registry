package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.BookerValidationErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerPrisonerVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ErrorResponseDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.LinkVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonVisitorRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.VisitorRequestsCountByPrisonCodeDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsService

const val PUBLIC_BOOKER_PRISONER_VISITOR_REQUESTS_PATH: String = "/public/booker/{bookerReference}/permitted/prisoners/{prisonerId}/permitted/visitors/request"

const val GET_VISITOR_REQUESTS_BY_BOOKER_REFERENCE: String = "/public/booker/{bookerReference}/permitted/visitors/requests"

const val GET_SINGLE_VISITOR_REQUEST: String = "/visitor-requests/{requestReference}"
const val APPROVE_VISITOR_REQUEST: String = "/visitor-requests/{requestReference}/approve"

const val GET_VISITOR_REQUESTS_BY_PRISON_CODE: String = "/prison/{prisonCode}/visitor-requests"
const val GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE: String = "/prison/{prisonCode}/visitor-requests/count"

@RestController
class VisitorRequestsController(
  val visitorRequestsService: VisitorRequestsService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PostMapping(PUBLIC_BOOKER_PRISONER_VISITOR_REQUESTS_PATH)
  @Operation(
    summary = "Submit a request to add a visitor to a booker's prisoner permitted visitor list",
    description = "Submit a request to add a visitor to a booker's prisoner permitted visitor list",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = AddVisitorToBookerPrisonerRequestDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Request submitted to add a visitor to a booker's prisoner permitted visitor list",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to submit a visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booker / Prisoner not found for visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "422",
        description = "Visitor request validation failed",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = BookerValidationErrorResponse::class))],
      ),
    ],
  )
  fun submitRequestAddVisitorToBookerPrisoner(
    @PathVariable
    bookerReference: String,
    @PathVariable
    prisonerId: String,
    @RequestBody
    @Valid
    visitorRequestDto: AddVisitorToBookerPrisonerRequestDto,
  ): ResponseEntity<String> {
    visitorRequestsService.submitVisitorRequest(bookerReference, prisonerId, visitorRequestDto)
    return ResponseEntity.status(HttpStatus.CREATED.value()).build()
  }

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISITOR_REQUESTS_BY_BOOKER_REFERENCE)
  @Operation(
    summary = "Get all active visitor requests for a booker",
    description = "Get all active visitor requests for a booker",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns a booker's active visitor requests",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get booker's active visitor requests",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get booker's awaiting visitor requests.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booker not found for booker reference.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun getActiveVisitorRequests(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): List<BookerPrisonerVisitorRequestDto> = visitorRequestsService.getActiveVisitorRequests(bookerReference)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISITOR_REQUESTS_COUNT_BY_PRISON_CODE)
  @Operation(
    summary = "Get a count of all visitor requests for a prison via prison code",
    description = "Get a count of all visitor requests for a prison via prison code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Count successfully returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get count of visitor requests for prison.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get count of visitor requests for prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorRequestsCountByPrisonCode(
    @PathVariable(value = "prisonCode", required = true)
    @NotBlank
    prisonCode: String,
  ): VisitorRequestsCountByPrisonCodeDto = visitorRequestsService.getVisitorRequestsCountByPrisonCode(prisonCode)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @GetMapping(GET_VISITOR_REQUESTS_BY_PRISON_CODE)
  @Operation(
    summary = "Get a list of all active visitor requests for a prison via prison code",
    description = "Get a list of all active visitor requests for a prison via prison code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "list successfully returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get list of active visitor requests for prison.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get list of active visitor requests for prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getVisitorRequestsByPrisonCode(
    @PathVariable(value = "prisonCode", required = true)
    @NotBlank
    prisonCode: String,
  ): List<PrisonVisitorRequestDto> = visitorRequestsService.getVisitorRequestsByPrisonCode(prisonCode)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @GetMapping(GET_SINGLE_VISITOR_REQUEST)
  @Operation(
    summary = "Get a single visitor request, given a request reference",
    description = "Get a single visitor request, given a request reference",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "single request successfully returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get a single visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get a single visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booker not found or visitor request not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getSingleVisitorRequest(
    @PathVariable
    requestReference: String,
  ): PrisonVisitorRequestDto = visitorRequestsService.getVisitorRequest(requestReference)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(APPROVE_VISITOR_REQUEST)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Approve visitor request and link visitor to booker's prisoner.",
    description = "Approve visitor request and link visitor to booker's prisoner.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit request approved and visitor linked to booker's prisoner",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to approve visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to approve visitor request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Visitor request not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun approveVisitorRequest(
    @PathVariable
    requestReference: String,
    @RequestBody
    linkVisitorRequest: LinkVisitorRequestDto,
  ) = visitorRequestsService.approveAndLinkVisitorRequest(requestReference, linkVisitorRequest)
}
