package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.AuthDetailDto
import java.time.LocalDateTime

@Deprecated("no longer needed, to be removed")
@Entity
@Table(name = "auth_detail")
class AuthDetail(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(name = "count", nullable = false)
  var count: Int = 0,

  @Column(name = "one_login_sub", nullable = false)
  val oneLoginSub: String,

  @Column(name = "email", nullable = false)
  var email: String,

  @Column(name = "phone_number", nullable = true)
  var phoneNumber: String? = null,

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

  override fun toString(): String = this::class.simpleName + "(id=$id, created = $createTimestamp)"

  constructor(authDetailDto: AuthDetailDto) : this(
    oneLoginSub = authDetailDto.oneLoginSub,
    email = authDetailDto.email,
    phoneNumber = authDetailDto.phoneNumber,
  )
}
