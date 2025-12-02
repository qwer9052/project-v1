package com.project.message.service

import com.project.message.dto.ChannelDTO
import com.project.message.entity.ChannelEntity
import com.project.message.repository.ChannelRepository
import org.springframework.stereotype.Service

@Service
class ChannelService(private val channelRepository: ChannelRepository) {

    fun getChannels(): List<ChannelDTO> {
        return channelRepository.findAll().map { channelEntity ->
            channelEntity?.let {
                ChannelDTO(
                    id = channelEntity.id,
                    name = channelEntity.name
                )
            } as ChannelDTO
        }
    }

    fun createChannel(channelDTO: ChannelDTO) {
        channelRepository.save(ChannelEntity(name = channelDTO.name))
    }
}