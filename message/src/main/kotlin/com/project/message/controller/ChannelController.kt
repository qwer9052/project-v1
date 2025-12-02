package com.project.message.controller

import com.project.message.dto.ChannelDTO
import com.project.message.service.ChannelService
import org.springframework.web.bind.annotation.*


@RestController
class ChannelController(private val channelService: ChannelService) {
    @GetMapping("/channels/{id}")
    suspend fun getMessageById(@PathVariable id: String): String {
        return "asdasd"
    }

    @GetMapping("/channels")
    suspend fun getChannels(): List<ChannelDTO> {
        return channelService.getChannels()
    }

    @PostMapping("/channels")
    suspend fun createChannels(@RequestBody channelDTO: ChannelDTO) {
        return channelService.createChannel(channelDTO)
    }

}