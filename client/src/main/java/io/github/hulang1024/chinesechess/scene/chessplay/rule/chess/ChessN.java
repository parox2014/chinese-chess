package io.github.hulang1024.chinesechess.scene.chessplay.rule.chess;

import io.github.hulang1024.chinesechess.scene.chessplay.rule.ChessPosition;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.HostEnum;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.RoundGame;

/**
 * 马
 * @author Hu Lang
 */
public class ChessN extends AbstractChess {
    private static final int MAX_DISTANCE = 2;
    public ChessN(ChessPosition pos, HostEnum host) {
        super(pos, host);
    }

    @Override
    public boolean canGoTo(ChessPosition destPos, RoundGame game) {
        // 马走“日”，蹩马腿
        int rowOffset = destPos.row - pos.row;
        int colOffset = destPos.col - pos.col;

        if (Math.abs(rowOffset) == MAX_DISTANCE && Math.abs(colOffset) == 1) {
            return game.getChessboard().isEmpty(pos.row + (rowOffset > 0 ? +1 : -1), pos.col);
        } else if (Math.abs(rowOffset) == 1 && Math.abs(colOffset) == MAX_DISTANCE) {
            return game.getChessboard().isEmpty(pos.row, pos.col + (colOffset > 0 ? +1 : -1));
        } else {
            return false;
        }
    }
}
