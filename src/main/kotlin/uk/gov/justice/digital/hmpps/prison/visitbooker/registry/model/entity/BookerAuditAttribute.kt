package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.BookerAuditAttributeTypes

@Entity
@Table(name = "booker_audit_attribute")
class BookerAuditAttribute(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  open val id: Long = 0,

  @Column(name = "booker_audit_id", unique = false, nullable = false)
  val bookerAuditId: Long,

  @Column(name = "attribute_name", nullable = false)
  @Enumerated(EnumType.STRING)
  val attributeName: BookerAuditAttributeTypes,

  @Column(name = "attribute_value", nullable = false)
  val attributeValue: String,

  @ManyToOne
  @JoinColumn(name = "booker_audit_id", updatable = false, insertable = false)
  val bookerAudit: BookerAudit,
)
