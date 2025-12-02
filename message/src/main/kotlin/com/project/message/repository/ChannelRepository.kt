package com.project.message.repository

import com.project.message.entity.ChannelEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ChannelRepository : JpaRepository<ChannelEntity, UUID> {
}