package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.enums

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class LanguagePreferenceConverter : AttributeConverter<LanguagePreference, String> {

  override fun convertToDatabaseColumn(attribute: LanguagePreference?): String? = attribute?.code

  override fun convertToEntityAttribute(dbData: String?): LanguagePreference? = dbData?.let { LanguagePreference.from(it) }
}
