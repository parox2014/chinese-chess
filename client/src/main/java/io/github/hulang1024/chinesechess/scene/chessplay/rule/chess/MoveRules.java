package io.github.hulang1024.chinesechess.scene.chessplay.rule.chess;

import io.github.hulang1024.chinesechess.scene.chessplay.rule.Chess;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.ChessPosition;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.HostEnum;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.RoundGame;

/**
 * 移动规则函数
 * @author Hu Lang
 */
/*package*/ class MoveRules {
    public static final int MAX_DISTANCE = 10;

    /**
     * 判定是直线移动
     * @param rowOffset
     * @param colOffset
     * @param maxDistance
     * @return
     */
    public static boolean isStraightLineMove(int rowOffset, int colOffset, int maxDistance) {
        rowOffset = Math.abs(rowOffset);
        colOffset = Math.abs(colOffset);
        return (rowOffset <= maxDistance && colOffset == 0) || (rowOffset == 0 && colOffset <= maxDistance);
    }

    /**
     * 目标位置是否在本方阵地内
     * @param game
     * @param host 棋子方
     * @param destPos 目标位置
     * @return
     */
    public static boolean isInBoundary(RoundGame game, HostEnum host, ChessPosition destPos) {
        return game.isHostAtChessboardTop(host) ? destPos.row < 5 : destPos.row > 4;
    }

    /**
     * 判定目标位置是否在九宫格内
     * @param chess
     * @param destPos
     * @return
     */
    public static boolean isInKingHome(Chess chess, ChessPosition destPos, RoundGame game) {
        return (3 <= destPos.col && destPos.col <= 5)
            && (game.isHostAtChessboardTop(chess.host())
                ? (0 <= destPos.row && destPos.row <= 2)
                : (7 <= destPos.row && destPos.row <= 9));
    }
}
