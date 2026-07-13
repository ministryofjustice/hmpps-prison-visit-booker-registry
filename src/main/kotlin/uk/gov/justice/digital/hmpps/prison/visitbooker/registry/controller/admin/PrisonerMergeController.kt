package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonerMergeBatchRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PrisonerMergeRequestDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.PrisonerMergeService

const val PRISONER_MERGE_PATH: String = "/public/prisoner/merge"
const val PRISONER_MERGE_BATCH_PATH: String = "$PRISONER_MERGE_PATH/batch"

@RestController
class PrisonerMergeController(
  private val prisonerMergeService: PrisonerMergeService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PostMapping(PRISONER_MERGE_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Manually merge a prisoner number",
    description = "Updates records from an old prisoner number to a new prisoner number.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner number merge processed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to merge a prisoner number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to merge a prisoner number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun mergePrisoner(
    @RequestBody
    @Valid
    prisonerMergeRequestDto: PrisonerMergeRequestDto,
  ) = prisonerMergeService.mergePrisoner(
    oldPrisonerNumber = prisonerMergeRequestDto.oldPrisonerNumber,
    newPrisonerNumber = prisonerMergeRequestDto.newPrisonerNumber,
  )

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PostMapping(PRISONER_MERGE_BATCH_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Manually merge prisoner numbers in a batch",
    description = "Processes each prisoner number merge in its own transaction.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prisoner number merges processed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to merge prisoner numbers",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to merge prisoner numbers",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun mergePrisoners(
    @RequestBody
    @Valid
    prisonerMergeBatchRequestDto: PrisonerMergeBatchRequestDto,
  ) {
    prisonerMergeBatchRequestDto.prisonerMerges.forEach {
      prisonerMergeService.mergePrisoner(
        oldPrisonerNumber = it.oldPrisonerNumber,
        newPrisonerNumber = it.newPrisonerNumber,
      )
    }
  }
}
