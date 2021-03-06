package io.github.hulang1024.chinesechessserver.message.client.room;

import io.github.hulang1024.chinesechessserver.message.ClientMessage;
import io.github.hulang1024.chinesechessserver.message.client.MessageType;
import lombok.Data;

@Data
@MessageType("room.create")
public class RoomCreate extends ClientMessage {
    private String roomName;
    private String password;
}