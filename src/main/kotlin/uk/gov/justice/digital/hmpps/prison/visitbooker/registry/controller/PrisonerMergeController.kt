package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.PrisonerMergeService

const val PRISONER_MERGE_PATH: String = "/public/booker/prisoner/{oldPrisonerId}/merge/{newPrisonerId}"

@RestController
class PrisonerMergeController(
  val prisonerMergeService: PrisonerMergeService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VSIP_ORCHESTRATION_SERVICE')")
  @PostMapping(PRISONER_MERGE_PATH)
  @Operation(
    summary = "Handle a merge of a prisoner",
    description = "Handle a merge of a prisoner by updating the prisoner number in the booker prisoner table.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Request successfully processed",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to submit a merge request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun mergePrisoner(
    @PathVariable
    oldPrisonerId: String,
    @PathVariable
    newPrisonerId: String,
  ): ResponseEntity<Void> {
    prisonerMergeService.mergePrisoner(oldPrisonerNumber = oldPrisonerId, newPrisonerNumber = newPrisonerId)
    return ResponseEntity.ok().build()
  }
}
