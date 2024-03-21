package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import uk.gov.justice.digital.hmpps.oneloginuserregistry.dto.AuthDetailDto
import java.time.LocalDateTime

@Entity
@Table(name = "auth_detail")
class AuthDetail(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(name = "auth_reference", nullable = false)
  val authReference: String,

  @Column(name = "auth_email", nullable = false)
  val authEmail: String,

  @Column(name = "auth_phone_number", nullable = true)
  val authPhoneNumber: String? = null,

) {

  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AuthDetail

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String {
    return this::class.simpleName + "(id=$id, created = $createTimestamp)"
  }

  constructor(authDetailDto: AuthDetailDto) : this(
    authReference = authDetailDto.authReference,
    authEmail = authDetailDto.authEmail,
    authPhoneNumber = authDetailDto.authPhoneNumber,
  )
}
