package io.github.hulang1024.chinesechess.client.message.client.room;

import io.github.hulang1024.chinesechess.client.message.ClientMessage;
import lombok.Data;

@Data
public class RoomCreate extends ClientMessage {
    private String roomName;


    public RoomCreate() {
      super("room.create");
    }
}