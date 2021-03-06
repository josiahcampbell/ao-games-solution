package edu.gvsu.cis.campbjos.connectfour.model;

import org.junit.Test;

import static edu.gvsu.cis.campbjos.connectfour.model.GameRunner.TIMEOUT_FOUR_MOVES;
import static edu.gvsu.cis.campbjos.connectfour.model.GameRunner.playOnBoard;
import static edu.gvsu.cis.campbjos.connectfour.model.GameState.PIECE_COUNT_TO_WIN;
import static edu.gvsu.cis.campbjos.connectfour.model.GridHelper.createEmptyGrid;
import static edu.gvsu.cis.campbjos.connectfour.model.Player.PLAYER_ONE_VALUE;
import static org.junit.Assert.assertFalse;

public class GameVerticalTest {

    @Test(timeout = TIMEOUT_FOUR_MOVES)
    public void playGameToVerticalWin() {
        Board currentBoard = Board.createFromJson(createEmptyGrid());
        final Player playerOne = new Player(PLAYER_ONE_VALUE);
        final Player playerTwo = Player.nextPlayer(playerOne);
        final int firstColumn = 0;


        for (int move = 0; move < PIECE_COUNT_TO_WIN; move++) {
            currentBoard = playOnBoard(currentBoard, playerTwo, firstColumn);
        }

        GameState gameState = new GameState(currentBoard, playerOne);

        assertFalse(gameState.isWinner(playerTwo));
    }

}
