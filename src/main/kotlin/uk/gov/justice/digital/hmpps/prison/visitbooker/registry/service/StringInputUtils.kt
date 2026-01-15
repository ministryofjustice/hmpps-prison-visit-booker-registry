package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import java.text.Normalizer

@Component
class StringInputUtils {
  fun sanitiseText(text: String): String = replaceSpecialCharacters(getNormalisedText(text.trim()))

  private fun replaceSpecialCharacters(text: String): String = text.filter { it.isLetter() }

  private fun getNormalisedText(text: String): String {
    val normalisedText = if (!Normalizer.isNormalized(text, Normalizer.Form.NFKD)) {
      StringUtils.stripAccents(text)
    } else {
      text
    }

    return normalisedText
  }
}
