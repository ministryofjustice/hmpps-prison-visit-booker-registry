package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "permitted_prisoner")
class PermittedPrisoner(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(name = "booker_id", nullable = false)
  val bookerId: Long,

  @ManyToOne
  @JoinColumn(name = "booker_id", updatable = false, insertable = false)
  val booker: Booker,

  @Column(name = "prisoner_id", nullable = false)
  val prisonerId: String,

  @Column(name = "prison_code", nullable = false)
  var prisonCode: String,

  @Column(name = "active", nullable = false)
  var active: Boolean,

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "permittedPrisoner", orphanRemoval = true)
  val permittedVisitors: MutableList<PermittedVisitor> = mutableListOf(),
) {

  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as PermittedPrisoner

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = this::class.simpleName + "(id=$id, created = $createTimestamp)"
}
