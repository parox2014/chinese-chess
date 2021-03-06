package io.github.hulang1024.chinesechessserver.listener;

import java.util.ArrayList;
import java.util.List;

import io.github.hulang1024.chinesechessserver.convert.RoomConvert;
import io.github.hulang1024.chinesechessserver.convert.UserConvert;
import io.github.hulang1024.chinesechessserver.domain.Room;
import io.github.hulang1024.chinesechessserver.domain.SessionUser;
import io.github.hulang1024.chinesechessserver.domain.chat.ChatMessage;
import io.github.hulang1024.chinesechessserver.domain.chinesechess.rule.Chess;
import io.github.hulang1024.chinesechessserver.domain.chinesechess.rule.ChessHost;
import io.github.hulang1024.chinesechessserver.domain.chinesechess.rule.ChessboardState;
import io.github.hulang1024.chinesechessserver.entity.User;
import io.github.hulang1024.chinesechessserver.message.client.spectator.JoinWatchMsg;
import io.github.hulang1024.chinesechessserver.message.client.spectator.SpectatorLeaveReqMsg;
import io.github.hulang1024.chinesechessserver.message.server.spectator.SpectatorLeaveMsg;
import io.github.hulang1024.chinesechessserver.message.server.spectator.RoomRoundStateMsg;
import io.github.hulang1024.chinesechessserver.message.server.spectator.SpectatorJoinMsg;
import io.github.hulang1024.chinesechessserver.service.RoomService;
import io.github.hulang1024.chinesechessserver.service.UserSessionService;

public class SpectatorMessageListener extends MessageListener {
    private RoomService roomService = new RoomService();
    private UserSessionService userSessionService = new UserSessionService();

    @Override
    public void init() {
        addMessageHandler(JoinWatchMsg.class, this::onJoinWatch);
        addMessageHandler(SpectatorLeaveReqMsg.class, this::onLeave);

    }

    private void onJoinWatch(JoinWatchMsg msg) {
        SessionUser spectator = userSessionService.getUserBySession(msg.getSession());
        Room room = roomService.getById(msg.getRoomId());
        RoomRoundStateMsg stateMsg = new RoomRoundStateMsg();

        if (room == null) {
            stateMsg.setCode(2);
            send(stateMsg, msg.getSession());
            return;
        }

        if (room.getUserCount() < 2) {
            stateMsg.setCode(3);
            send(stateMsg, msg.getSession());
            return;
        }
        
        room.getSpectators().add(spectator);
        room.getChatChannel().joinUser(spectator);
        spectator.setSpectatingRoom(room);

        // 发送观众当前房间信息和棋局状态
        stateMsg.setRoom(new RoomConvert().toRoomInfo(room));
        if (room.getRound() != null) {
            if (room.getRound().getActiveChessHost() != null) {
                stateMsg.setActiveChessHost(room.getRound().getActiveChessHost().code());
            }
            stateMsg.setChesses(toStateChesses(room.getRound().getChessboardState()));
            stateMsg.setActionStack(room.getRound().getActionStack());
        }
        send(stateMsg, msg.getSession());

        // 发送给房间玩家用户观众信息
        SpectatorJoinMsg joinMsg = new SpectatorJoinMsg();
        joinMsg.setUser(new UserConvert().toRoomUserInfo(spectator));
        joinMsg.setSpectatorCount(room.getSpectators().size());
        room.getUsers().forEach(user -> {
            send(joinMsg, user.getSession());
        });
        room.getSpectators().forEach(user -> {
            send(joinMsg, user.getSession());
        });

        // 发送消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setFromUser(User.SYSTEM_USER);
        chatMessage.setContent(spectator.getUser().getNickname() + " 加入观看");
        room.getChatChannel().sendMessage(chatMessage);
    }

    private void onLeave(SpectatorLeaveReqMsg msg) {
        SessionUser spectator = userSessionService.getUserBySession(msg.getSession());
        Room room = spectator.getSpectatingRoom();
        
        room.getSpectators().remove(spectator);
        room.getChatChannel().removeUser(spectator);
        spectator.setSpectatingRoom(null);

        SpectatorLeaveMsg leaveMsg = new SpectatorLeaveMsg();
        leaveMsg.setUser(new UserConvert().toRoomUserInfo(spectator));
        leaveMsg.setSpectatorCount(room.getSpectators().size());
        room.getUsers().forEach(user -> {
            send(leaveMsg, user.getSession());
        });
        room.getSpectators().forEach(user -> {
            send(leaveMsg, user.getSession());
        });

        // 发送消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setFromUser(User.SYSTEM_USER);
        chatMessage.setContent(spectator.getUser().getNickname() + " 离开观看");
        room.getChatChannel().sendMessage(chatMessage);
    }

    private List<RoomRoundStateMsg.Chess> toStateChesses(ChessboardState chessboardState) {
        List<RoomRoundStateMsg.Chess> chesses = new ArrayList<>();
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Chess chess = chessboardState.chessAt(r, c);
                if (chess != null) {
                    RoomRoundStateMsg.Chess sChess = new RoomRoundStateMsg.Chess();
                    sChess.setHost(chess.chessHost == ChessHost.RED ? 1 : 2);
                    sChess.setType(chess.type.name().charAt(0));
                    sChess.setRow(r);
                    sChess.setCol(c);
                    chesses.add(sChess);
                }
            }
        }
        return chesses;
    }
}