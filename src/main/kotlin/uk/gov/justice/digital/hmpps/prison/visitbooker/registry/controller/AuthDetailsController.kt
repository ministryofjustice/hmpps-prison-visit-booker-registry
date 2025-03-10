package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerReference
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ErrorResponseDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.AuthService

const val AUTH_DETAILS_CONTROLLER_PATH: String = "/register/auth"

@RestController
@Validated
@Tag(name = "")
@RequestMapping(name = "AuthDetails Resource", produces = [MediaType.APPLICATION_JSON_VALUE])
class AuthDetailsController(
  private val authService: AuthService,
) {

  @PreAuthorize("hasAnyRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @PutMapping(AUTH_DETAILS_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Adds a booker entry for a user authorised on GOV.UK one login if it does not exist and / or returns the booker reference for the booker.",
    description = "Creates a booker to allow access to public visits  if it does not exist and / or returns the booker reference for the given auth details.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Booker successfully added and / or booker reference returned.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "An error occurred whilst adding the booker.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun bookerAuthorisation(@RequestBody @Valid authDetailDto: AuthDetailDto): BookerReference = BookerReference(authService.authoriseBooker(authDetailDto))
}
