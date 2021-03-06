package io.github.hulang1024.chinesechessserver.message.server.lobby;

import java.util.List;

import io.github.hulang1024.chinesechessserver.message.ServerMessage;
import io.github.hulang1024.chinesechessserver.message.server.room.RoomInfo;
import lombok.Data;

/**
 * 搜索房间结果
 */
@Data
public class SearchRoomsResult extends ServerMessage {
    /**
     * 房间列表
     */
    private List<RoomInfo> rooms;

    public SearchRoomsResult() {
        super("lobby.search_rooms");
    }
}
