package com.project.message.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import java.util.*


@Entity
@Table(name = "channel")
class ChannelEntity(
    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR) // DB에 CHAR(36)로 저장
    var id: UUID? = null,

    var name: String? = null
) {


}