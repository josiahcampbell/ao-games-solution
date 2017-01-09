package edu.gvsu.cis.campbjos.connectfour.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.gvsu.cis.campbjos.connectfour.model.Board.COLUMN_SIZE;
import static edu.gvsu.cis.campbjos.connectfour.model.Board.ROW_SIZE;
import static edu.gvsu.cis.campbjos.connectfour.model.Player.PLAYER_ONE_PIECE;
import static edu.gvsu.cis.campbjos.connectfour.model.Player.PLAYER_TWO_PIECE;

public class GameState {

    static final int PIECE_COUNT_TO_WIN = 4;
    static final int DRAW = 3;

    private final Board board;
    private final Player currentPlayer;
    private final Player opponent;
    private final int winner;
    private Attempt bestAttempt;

    private Set<Integer> availableMoves;

    GameState(Board board, Player currentPlayer) {
        this.board = board;
        this.currentPlayer = currentPlayer;
        this.opponent = Player.nextPlayer(currentPlayer);
        winner = checkForWinner();

    }

    public static GameState createFromJson(final String serializedBoard, final String playerValue) {
        return new GameState(
                Board.createFromJson(serializedBoard),
                new Player(playerValue));
    }

    private int checkForWinner() {
        List<Integer> wins = new ArrayList<>(4);

        boolean isPlayerOneWinner = false;
        boolean isPlayerTwoWinner = false;

        wins.add(checkVerticalWin());
        wins.add(checkHorizontalWin());
        wins.add(checkForwardDiagonalWin());
        wins.add(checkReverseDiagonalWin());

        for (int win : wins) {
            switch (win) {
                case PLAYER_ONE_PIECE:
                    isPlayerOneWinner = true;
                    break;
                case PLAYER_TWO_PIECE:
                    isPlayerTwoWinner = true;
                    break;
                default:
                    break;
            }
        }
        if (isPlayerOneWinner) {
            return PLAYER_ONE_PIECE;
        }
        if (isPlayerTwoWinner) {
            return PLAYER_TWO_PIECE;
        }
        if (isBoardFull()) {
            return DRAW;
        }

        return 0;
    }

    boolean isWinner(Player player) {
        return winner == player.getPiece();
    }

    boolean isOver() {
        return winner > 0;
    }

    private boolean isBoardFull() {
        return getAvailableMoves().isEmpty();
    }

    int getWinner() {
        return winner;
    }

    public int makeMove() {
        currentPlayer.runMinimax(this, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        int bestMove = currentPlayer.getBestPossibleMove();
        if (!getAvailableMoves().contains(bestMove)) {
            return new ArrayList<>(getAvailableMoves()).get(0);
        }
        return bestMove;
    }

    int getMaximumPieceCount() {
        return bestAttempt.contiguousCount;
    }

    /**
     * Check from the bottom of the board to the top
     *
     * @return a list of available spaces
     */
    Set<Integer> getAvailableMoves() {
        if (availableMoves != null) {
            return availableMoves;
        }
        availableMoves = new HashSet<>();
        for (int row = ROW_SIZE - 1; row >= 0; row--) {
            for (int column = 0; column < COLUMN_SIZE; column++) {
                if (board.isOpenSpace(row, column)) {
                    availableMoves.add(column);
                }
            }
        }
        return availableMoves;
    }

    GameState getChildState(int column) {
        Board nextBoard = board.duplicate();
        nextBoard.placePiece(currentPlayer, column);
        return new GameState(nextBoard, opponent);
    }

    Player getCurrentPlayer() {
        return currentPlayer;
    }

    private int checkVerticalWin() {
        return checkStraightLineWin(false);
    }

    private int checkHorizontalWin() {
        return checkStraightLineWin(true);
    }

    private int checkStraightLineWin(boolean isHorizontal) {
        int contiguousPieces = 0;
        int pieceCountAtMost = 0;
        int rowBound;
        int columnBound;
        if (isHorizontal) {
            rowBound = ROW_SIZE;
            columnBound = COLUMN_SIZE - 1;
        } else {
            rowBound = ROW_SIZE - 1;
            columnBound = COLUMN_SIZE;
        }
        for (int row = 0; row < rowBound; row++) {
            for (int column = 0; column < columnBound; column++) {
                int currentPiece = board.at(row, column);
                int nextPiece;
                if (isHorizontal) {
                    nextPiece = board.at(row, column + 1);
                } else {
                    nextPiece = board.at(row + 1, column);
                }
                int nextSum = getSumOfPieces(contiguousPieces,
                        currentPiece,
                        nextPiece);
                if (currentPiece == currentPlayer.getPiece()) {
                    pieceCountAtMost = updatePieceCountAtMost(pieceCountAtMost, nextSum, contiguousPieces);
                    bestAttempt = new Attempt(currentPlayer.getPiece(), pieceCountAtMost);
                }
                contiguousPieces = nextSum;

                if (contiguousPieces >= PIECE_COUNT_TO_WIN) {
                    return currentPiece;
                }
            }
        }

        return 0;
    }

    private int checkForwardDiagonalWin() {
        return checkDiagonalWin(true);
    }

    private int checkReverseDiagonalWin() {
        return checkDiagonalWin(false);
    }

    private int checkDiagonalWin(boolean includeForwardOffset) {
        int diagonalSum = ROW_SIZE + COLUMN_SIZE - 1;
        int pieceCountAtMost = 0;
        for (int diagonalSlice = 0; diagonalSlice < diagonalSum; diagonalSlice++) {

            int startOffset = getDiagonalStartOffset(diagonalSlice);
            int endOffset = getDiagonalEndOffset(diagonalSlice);

            int accessRow = diagonalSlice - startOffset;

            List<Integer> diagonalPieces = new ArrayList<>();
            while (accessRow >= endOffset) {
                int row;
                if (includeForwardOffset) {
                    row = ROW_SIZE - accessRow - 1;
                } else {
                    row = accessRow;
                }

                int column = diagonalSlice - accessRow;
                diagonalPieces.add(board.at(row, column));
                accessRow--;
            }
            int size = diagonalPieces.size();

            if (size < PIECE_COUNT_TO_WIN) {
                continue;
            }
            int contiguousPieces = 0;
            for (int index = 0; index < size - 1; index++) {
                int piece = diagonalPieces.get(index);
                int nextPiece = diagonalPieces.get(index + 1);
                int nextSum = getSumOfPieces(contiguousPieces, piece, nextPiece);
                if (piece == currentPlayer.getPiece()) {
                    pieceCountAtMost = updatePieceCountAtMost(pieceCountAtMost, nextSum, contiguousPieces);
                    bestAttempt = new Attempt(currentPlayer.getPiece(), pieceCountAtMost);
                }
                contiguousPieces = nextSum;

                if (contiguousPieces >= PIECE_COUNT_TO_WIN) {
                    return piece;
                }
            }
        }

        return 0;
    }

    private int updatePieceCountAtMost(final int pieceCountAtMost, final int nextSum, final int
            contiguousPieces) {
        if (nextSum == 0 && contiguousPieces > 0) {
            if (contiguousPieces > pieceCountAtMost) {
                return contiguousPieces;
            }
        }
        return pieceCountAtMost;
    }

    private int getDiagonalStartOffset(int diagonalSlice) {
        if (diagonalSlice < ROW_SIZE) {
            return 0;
        }
        return diagonalSlice - ROW_SIZE + 1;
    }

    private int getDiagonalEndOffset(int diagonalSlice) {
        if (diagonalSlice < COLUMN_SIZE) {
            return 0;
        }
        return diagonalSlice - COLUMN_SIZE + 1;
    }

    private int getSumOfPieces(final int runningTotal,
                               final int currentPiece, final int nextPiece) {
        int contiguousPieces = runningTotal;

        if (currentPiece == 0) {
            return contiguousPieces;
        }
        if (contiguousPieces == 0) {
            contiguousPieces++;
        }
        if (nextPiece == currentPiece) {
            contiguousPieces++;
        } else {
            contiguousPieces = 0;
        }

        return contiguousPieces;
    }
}
