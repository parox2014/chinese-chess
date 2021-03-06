package io.github.hulang1024.chinesechess.scene.chessplay;

import io.github.hulang1024.chinesechess.ChineseChessClient;
import io.github.hulang1024.chinesechess.message.MessageHandler;
import io.github.hulang1024.chinesechess.message.client.chessplay.ChessEat;
import io.github.hulang1024.chinesechess.message.client.chessplay.ChessMove;
import io.github.hulang1024.chinesechess.message.client.chessplay.ChessPlayReady;
import io.github.hulang1024.chinesechess.message.client.room.RoomLeave;
import io.github.hulang1024.chinesechess.message.server.chessplay.ChessEatResult;
import io.github.hulang1024.chinesechess.message.server.chessplay.ChessMoveResult;
import io.github.hulang1024.chinesechess.message.server.chessplay.ChessPlayReadyResult;
import io.github.hulang1024.chinesechess.message.server.chessplay.ChessPlayRoundStart;
import io.github.hulang1024.chinesechess.message.server.lobby.LobbyRoom;
import io.github.hulang1024.chinesechess.message.server.lobby.LobbyRoom.LobbyRoomPlayerInfo;
import io.github.hulang1024.chinesechess.message.server.room.RoomJoinResult;
import io.github.hulang1024.chinesechess.message.server.room.RoomLeaveResult;
import io.github.hulang1024.chinesechess.scene.AbstractScene;
import io.github.hulang1024.chinesechess.scene.SceneContext;
import io.github.hulang1024.chinesechess.scene.SessionManager;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.*;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.chess.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.function.BiConsumer;


/**
 * 游戏下棋主场景
 * @author Hu Lang
 */
public class OnlineChessPlayScene extends AbstractScene implements RoundGame {
    private ChineseChessClient client = ChineseChessClient.getInstance();
    private MessageHandler<RoomJoinResult> roomJoinMessageHandler;
    private MessageHandler<RoomLeaveResult> roomLeaveMessageHandler;
    private MessageHandler<ChessPlayReadyResult> readyMessageHandler;
    private MessageHandler<ChessPlayRoundStart> roundStartMessageHandler;
    private MessageHandler<ChessMoveResult> chessMoveMessageHandler;
    private MessageHandler<ChessEatResult> chessEatMessageHandler;

    private DrawableChessboard chessboard = new DrawableChessboard(this);
    /** 本方 */
    private HostEnum host;
    private HostEnum activeHost;
    private DrawableChess lastSelected;

    public OnlineChessPlayScene(SceneContext context, LobbyRoom room) {
        super(context);

        getChildren().add(chessboard);

        VBox vbox = new VBox();
        getChildren().add(vbox);

        LobbyRoomPlayerInfo thisPlayer = SessionManager.player;

        PlayerInfoContainer otherPlayerInfoContainer = new PlayerInfoContainer();
        if (room.getPlayerCount() > 1) {
            LobbyRoomPlayerInfo otherPlayer = room.getPlayers().stream()
                .filter(player -> thisPlayer.getId() != player.getId())
                .findAny().get();
            otherPlayerInfoContainer.load(otherPlayer);
        }
        vbox.getChildren().add(otherPlayerInfoContainer);
        
        PlayerInfoContainer thisPlayerInfoContainer = new PlayerInfoContainer();
        thisPlayerInfoContainer.load(thisPlayer);
        vbox.getChildren().add(thisPlayerInfoContainer);

        HBox roomOpContainer = new HBox();
        roomOpContainer.setPadding(new Insets(0, 0, 0, 16));
        vbox.getChildren().add(roomOpContainer);

        ReadyButton readyButton = new ReadyButton(false);
        readyButton.setOnMouseClicked((event) -> {
            client.send(new ChessPlayReady());
        });
        roomOpContainer.getChildren().add(readyButton);

        Button backButton = new Button("离开");
        backButton.setMinWidth(80);
        backButton.setMinHeight(30);
        backButton.setOnMouseClicked((event) -> {
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.initOwner(this.context.getPrimaryStage());
            alert.setTitle("确认离开");
            alert.setHeaderText("");
            alert.setContentText("真的要离开吗？");
            ButtonType result = alert.showAndWait().get();
            if (result == ButtonType.OK) {
                client.send(new RoomLeave());
            }
        });
        roomOpContainer.getChildren().add(backButton);

        client.addMessageHandler(RoomJoinResult.class, roomJoinMessageHandler = (message) -> {
            if (message.getCode() != 0) {
                return;
            }
            Platform.runLater(() -> {
                this.context.getPrimaryStage().requestFocus();
                otherPlayerInfoContainer.load(message.getPlayer());
            });
        });

        client.addMessageHandler(RoomLeaveResult.class, roomLeaveMessageHandler = (message) -> {
            if (message.getCode() != 0) {
                return;
            }
            Platform.runLater(() -> {
                if (message.getPlayer().getId() == thisPlayer.getId()) {
                    popScene();
                } else {
                    otherPlayerInfoContainer.clear();
                    chessboard.getChessList().forEach(chess -> {
                        ((DrawableChess)chess).setDisable(true);
                    });
                    chessboard.setDisable(true);
                    readyButton.setVisible(true);

                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.initOwner(this.context.getPrimaryStage());
                    alert.setTitle("提示");
                    alert.setHeaderText("");
                    alert.setContentText("对方已离开房间");
                    alert.show();
                }
            });
        });

        client.addMessageHandler(ChessPlayReadyResult.class, readyMessageHandler = (message) -> {
            if (message.getCode() != 0) {
                return;
            }

            Platform.runLater(() -> {
                if (message.getPlayer().getId() == thisPlayer.getId()) {
                    readyButton.toggleReady();
                    thisPlayerInfoContainer.setReadyState(message.getPlayer().isReadyed());
                } else {
                    otherPlayerInfoContainer.setReadyState(message.getPlayer().isReadyed());
                }
            });
        });

        client.addMessageHandler(ChessPlayRoundStart.class, roundStartMessageHandler = (message) -> {
            if (message.getCode() != 0) {
                return;
            }

            Platform.runLater(() -> {
                readyButton.setVisible(false);
                HostEnum thisHost = HostEnum.fromCode(message.getHost());
                thisPlayerInfoContainer.setHost(thisHost);
                otherPlayerInfoContainer.setHost(thisHost.reverse());
                startRound(message);
            });
        });
    }

    public void startRound(ChessPlayRoundStart roundStart) {
        host = HostEnum.fromCode(roundStart.getHost());
        activeHost = null;
        lastSelected = null;
        chessboard.setDisable(false);
        resetChessLayout();
        turnHost();

        // 重新开始一局，删除上次的绑定
        client.removeMessageHandler(ChessMoveResult.class, chessMoveMessageHandler);

        client.addMessageHandler(ChessMoveResult.class, chessMoveMessageHandler = (message) -> {
            boolean isOtherHost = HostEnum.fromCode(message.getHost()) != host;
            DrawableChess source = (DrawableChess)chessboard.chessAt(
                PositionUtils.remoteRowToLocalRow(isOtherHost, message.getSourceChessRow()),
                PositionUtils.remoteColToLocalCol(isOtherHost, message.getSourceChessCol()));
            DrawableChess target = (DrawableChess)chessboard.chessAt(
                PositionUtils.remoteRowToLocalRow(isOtherHost, message.getTargetChessRow()),
                PositionUtils.remoteColToLocalCol(isOtherHost, message.getTargetChessCol()));
            Platform.runLater(() -> {
                // 清除高亮对方移动了哪个棋子
                chessboard.getChessList().forEach(chess -> {
                    if (((DrawableChess)chess).isSelected()) {
                        ((DrawableChess)chess).setSelected(false);
                    }
                });
                // 高亮对方移动了哪个棋子
                if (isOtherHost) {
                    source.setSelected(true);
                    target.setSelected(true);
                }

                this.context.getPrimaryStage().requestFocus();
                ChessPosition sourcePos = source.pos().copy();
                chessboard.removeChess(target);
                chessboard.moveChess(source, target.pos());
                target.setPos(sourcePos);
                chessboard.addChess(target);
                turnHost();
            });
        });

        client.removeMessageHandler(ChessEatResult.class, chessEatMessageHandler);
        client.addMessageHandler(ChessEatResult.class, chessEatMessageHandler = (message) -> {
            boolean isOtherHost = HostEnum.fromCode(message.getHost()) != host;
            DrawableChess source = (DrawableChess)chessboard.chessAt(
                PositionUtils.remoteRowToLocalRow(isOtherHost, message.getSourceChessRow()),
                PositionUtils.remoteColToLocalCol(isOtherHost, message.getSourceChessCol()));
            DrawableChess target = (DrawableChess)chessboard.chessAt(
                PositionUtils.remoteRowToLocalRow(isOtherHost, message.getTargetChessRow()),
                PositionUtils.remoteColToLocalCol(isOtherHost, message.getTargetChessCol()));
            Platform.runLater(() -> {
                this.context.getPrimaryStage().requestFocus();
                ChessPosition sourcePos = source.pos().copy();
                chessboard.removeChess(target);
                chessboard.moveChess(source, target.pos());
                DrawableChess ghostChess = new DrawableChess(new ChessGhost(sourcePos));
                setChessEventHandlers(ghostChess);
                chessboard.addChess(ghostChess);

                // 清除高亮对方移动了哪个棋子
                chessboard.getChessList().forEach(chess -> {
                    if (((DrawableChess)chess).isSelected()) {
                        ((DrawableChess)chess).setSelected(false);
                    }
                });
                // 高亮对方移动了哪个棋子
                if (isOtherHost) {
                    source.setSelected(true);
                    ghostChess.setSelected(true);
                }
                turnHost();
            });
        });
    }

    private void resetChessLayout() {
        BiConsumer<HostEnum, int[]> addChessGroup = (host, rows) -> {
            Arrays.stream(new Chess[]{
                new ChessR(new ChessPosition(rows[0], 0), host),
                new ChessN(new ChessPosition(rows[0], 1), host),
                new ChessM(new ChessPosition(rows[0], 2), host),
                new ChessG(new ChessPosition(rows[0], 3), host),
                new ChessK(new ChessPosition(rows[0], 4), host),
                new ChessG(new ChessPosition(rows[0], 5), host),
                new ChessM(new ChessPosition(rows[0], 6), host),
                new ChessN(new ChessPosition(rows[0], 7), host),
                new ChessR(new ChessPosition(rows[0], 8), host),
                new ChessC(new ChessPosition(rows[1], 1), host),
                new ChessC(new ChessPosition(rows[1], 7), host),
                new ChessS(new ChessPosition(rows[2], 0), host),
                new ChessS(new ChessPosition(rows[2], 2), host),
                new ChessS(new ChessPosition(rows[2], 4), host),
                new ChessS(new ChessPosition(rows[2], 6), host),
                new ChessS(new ChessPosition(rows[2], 8), host)
            }).forEach(chess -> {
                DrawableChess drawableChess = new DrawableChess(chess);
                setChessEventHandlers(drawableChess);
                chessboard.addChess(drawableChess);
            });
        };

        chessboard.clear();
        
        // 顶部方
        addChessGroup.accept(host.reverse(), new int[]{0, 2, 3});
        // 底部方
        addChessGroup.accept(host, new int[]{9, 7, 6});
        // 加空棋
        for (int row = 0; row < Chessboard.ROW_NUM; row++) {
            for (int col = 0; col < Chessboard.COL_NUM; col++) {
                if (chessboard.isEmpty(row, col)) {
                    DrawableChess ghostChess = new DrawableChess(
                        new ChessGhost(new ChessPosition(row, col)));
                    setChessEventHandlers(ghostChess);
                    chessboard.addChess(ghostChess);
                }
            }
        }
    }

    private void turnHost() {
        if (activeHost == null) {
            activeHost = HostEnum.RED;
        } else {
            activeHost = activeHost.reverse();
        }

        System.out.println("现在 " + (activeHost == HostEnum.BLACK ? "黑方" : "红方") + " 持棋");
        
        chessboard.setCursor(activeHost == host ? Cursor.DEFAULT : Cursor.WAIT);

        chessboard.getChessList().forEach(chess -> {
            // 如果当前是本方走，将敌方棋子禁用；否则，全部禁用
            ((DrawableChess)chess).setDisable(activeHost == host ? chess.host() != host : true);
        });
    }

    private void onGoTo(DrawableChess selected, DrawableChess target) {
        // 目标位置上是否有棋子
        if (target.getChess() instanceof ChessGhost) {
            // 目标位置无棋子
            // 判断目标位置是否可走
            if (selected.canGoTo(target.pos(), this)) {
                // 目标位置可走
                ChessMove chessMove = new ChessMove();
                chessMove.setHost(host.code());
                chessMove.setSourceChessRow(selected.pos().row);
                chessMove.setSourceChessCol(selected.pos().col);
                chessMove.setTargetChessRow(target.pos().row);
                chessMove.setTargetChessCol(target.pos().col);
                client.send(chessMove);
            } else {
                System.out.println("走法不符规则");
            }
        } else {
            if (target.host() != selected.host()) {
                // 目标位置上有敌方棋子
                if (selected.canGoTo(target.pos(), this)) {
                    // 目标位置棋子可吃
                    ChessEat chessEat = new ChessEat();
                    chessEat.setHost(host.code());
                    chessEat.setSourceChessRow(selected.pos().row);
                    chessEat.setSourceChessCol(selected.pos().col);
                    chessEat.setTargetChessRow(target.pos().row);
                    chessEat.setTargetChessCol(target.pos().col);
                    client.send(chessEat);
                } else {
                    System.out.println("走法不符规则");
                }
            } else {
                // 目标位置上有本方棋子
                System.out.println("走法不符规则");
            }
        }
        selected.setSelected(false);
        this.lastSelected = null;
    }

    private void setChessEventHandlers(DrawableChess eventDrawableChess) {
        if (eventDrawableChess.getChess() instanceof ChessGhost) {
            eventDrawableChess.setOnMouseClicked(event -> {
                if (lastSelected != null) {
                    onGoTo(lastSelected, eventDrawableChess);
                }
            });
            return;
        }
        eventDrawableChess.setOnMouseClicked(event -> {
            if (lastSelected == null) {
                // 如果当前没有任何棋子选中，现在是选择要走的棋子，只能先选中持棋方棋子
                if (eventDrawableChess.host() != host) {
                    return;
                }

                eventDrawableChess.setSelected(true);
                lastSelected = eventDrawableChess;

                // 将非持棋方的棋子全部启用（这样才能点击要吃的目标棋子）
                chessboard.getChessList().forEach(chess -> {
                    if (chess.host() != host) {
                        ((DrawableChess)chess).setDisable(false);
                    }
                });
            } else if (eventDrawableChess.isSelected() && eventDrawableChess.host() == host) {
                // 重复点击，取消选中
                eventDrawableChess.setSelected(false);
                lastSelected = null;
            } else {
                // 当选择了两个棋子（包括了空棋子），并且两个棋子属于不同棋方，可能要移动或者吃子
                if (eventDrawableChess.host() != lastSelected.host()) {
                    onGoTo(lastSelected, eventDrawableChess);
                } else {
                    chessboard.getChessList().forEach(chess -> {
                        if (((DrawableChess)chess).isSelected()) {
                            ((DrawableChess)chess).setSelected(false);
                        }
                    });
                    eventDrawableChess.setSelected(true);
                    lastSelected = eventDrawableChess;
                }
            }
        });
    }

    @Override
    public void onSceneExit() {
        client.removeMessageHandler(RoomJoinResult.class, roomJoinMessageHandler);
        client.removeMessageHandler(RoomLeaveResult.class, roomLeaveMessageHandler);
        client.removeMessageHandler(ChessPlayReadyResult.class, readyMessageHandler);
        client.removeMessageHandler(ChessPlayRoundStart.class, roundStartMessageHandler);
        client.removeMessageHandler(ChessMoveResult.class, chessMoveMessageHandler);
        client.removeMessageHandler(ChessEatResult.class, chessEatMessageHandler);
    }

    @Override
    public DrawableChessboard getChessboard() {
        return chessboard;
    }

    @Override
    public boolean isHostAtChessboardTop(HostEnum host) {
        // 本方总是在底部，对方总是在顶部
        return host != this.host;
    }
}
