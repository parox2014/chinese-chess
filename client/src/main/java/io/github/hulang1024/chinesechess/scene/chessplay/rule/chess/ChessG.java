package io.github.hulang1024.chinesechess.scene.chessplay.rule.chess;

import io.github.hulang1024.chinesechess.scene.chessplay.rule.ChessPosition;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.HostEnum;
import io.github.hulang1024.chinesechess.scene.chessplay.rule.RoundGame;

/**
 * 士
 * @author Hu Lang
 */
public class ChessG extends AbstractChess {
    public ChessG(ChessPosition pos, HostEnum host) {
        super(pos, host);
    }

    @Override
    public boolean canGoTo(ChessPosition destPos, RoundGame game) {
        // 只许沿九宫斜线走单步，可进可退
        return Math.abs(destPos.row - pos.row) == 1 && Math.abs(destPos.col - pos.col) == 1
            && MoveRules.isInKingHome(this, destPos, game);
    }
}
