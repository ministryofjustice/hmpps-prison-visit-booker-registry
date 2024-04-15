package uk.gov.justice.digital.hmpps.oneloginuserregistry.controller

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
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.ErrorResponseDto
import uk.gov.justice.digital.hmpps.oneloginuserregistry.service.AuthService

const val AUTH_DETAILS_CONTROLLER_PATH: String = "/register/auth"

@RestController
@Validated
@Tag(name = "")
@RequestMapping(name = "AuthDetails Resource", produces = [MediaType.APPLICATION_JSON_VALUE])
class AuthDetailsController(
  private val authService: AuthService,
) {

  @PreAuthorize("hasAnyRole('ROLE_VISIT_BOOKER_REGISTRY__PUBLIC_VISIT_BOOKING_UI')")
  @PutMapping(AUTH_DETAILS_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Authenticate one login details against pre populated bookers",
    description = "Authenticate one login details against pre populated bookers",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "One login details matched with pre populated booker",
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
  fun bookerAuthorisation(@RequestBody @Valid authDetailDto: AuthDetailDto): String {
    return authService.bookerAuthorisation(authDetailDto)
  }
}
