package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.BookerValidationErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AddVisitorToBookerPrisonerRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.VisitorRequestsService

const val PUBLIC_BOOKER_PRISONER_VISITOR_REQUESTS_PATH: String = "/public/booker/{bookerReference}/permitted/prisoners/{prisonerId}/permitted/visitors/request"

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
}
