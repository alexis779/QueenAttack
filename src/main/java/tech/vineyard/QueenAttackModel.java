package tech.vineyard;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import tech.vineyard.ip.IntegerProgrammingModel;
import tech.vineyard.ip.Optimizer;
import tech.vineyard.model.Input;
import tech.vineyard.model.Position;
import tech.vineyard.model.Queen;
import tech.vineyard.model.Wall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueenAttackModel implements IntegerProgrammingModel {

    private final long[] coefficients = new long[] { 1, -1 };

    private final Input input;
    private final Optimizer optimizer = new Optimizer();
    private final CpModel cpModel = optimizer.cpModel;

    private final int N;
    private final int C;

    /**
     * Grid
     */
    private final Position[][] positions;

    /**
     * Queens
     */
    private final List<Queen> queens = new ArrayList<>();

    /**
     * Queens grouped by color
     */
    private final List<List<Queen>> colorQueens = new ArrayList<>();

    /**
     * Initial queen position
     */
    private final Map<Queen, Position> queenPositions0 = new HashMap<>();

    /**
     * Walls
     */
    private final List<Wall> walls = new ArrayList<>();

    /**
     * Wall position
     */
    private final Map<Wall, Position> wallPositions = new HashMap<>();

    /**
     * Total number of moves.
     */
    private final int T;

    private final IntVar[][] color;

    private final IntVar[][] attackPairs;

    private final IntVar[] moveCosts;
    private final IntVar[] moveOffsets;

    public QueenAttackModel(Input input) {
        this.input = input;

        N = input.N();
        C = input.C();

        positions = new Position[N][N];
        IntStream.range(0, N)
                .forEach(r -> IntStream.range(0, N)
                        .forEach(c -> positions[r][c] = new Position(r, c)));

        IntStream.range(0, C+1)
                .forEach(c -> colorQueens.add(new ArrayList<>()));

        IntStream.range(0, N)
                .forEach(r -> IntStream.range(0, N)
                        .forEach(c -> addCell(r, c, input.grid()[r][c])));

        color = new IntVar[N][N];
        attackPairs = new IntVar[N][N];

        T = 1;
        moveOffsets = new IntVar[T];
        moveCosts = new IntVar[T];
    }

    private void addCell(int r, int c, int cellId) {
        switch (cellId) {
            case -1 -> {
                // wall
                Wall wall = new Wall(walls.size());
                walls.add(wall);
                wallPositions.put(wall, positions[r][c]);
            }
            case 0 -> {
                // empty
            }
            default -> {
                // queen
                Queen queen = new Queen(queens.size());
                queens.add(queen);
                colorQueens.get(cellId).add(queen);
                queenPositions0.put(queen, positions[r][c]);
            }
        }
    }

    @Override
    public void buildVariables() {
        Map<Integer, IntVar[]> row = new HashMap<>();
        IntStream.range(0, N)
                .forEach(x -> row.put(x, IntStream.range(0, N)
                                .mapToObj(y -> color[x][y])
                                .toArray(IntVar[]::new)));

        Map<Integer, IntVar[]> column = new HashMap<>();
        IntStream.range(0, N)
                .forEach(y -> column.put(y, IntStream.range(0, N)
                        .mapToObj(x -> color[x][y])
                        .toArray(IntVar[]::new)));

        Map<Integer, IntVar[]> leftDiagonal = new HashMap<>();
        IntStream.range(0, 2*N-1)
                .forEach(k -> leftDiagonal.put(k, IntStream.range(0, N)
                        .filter(x -> 0 <= k-x && k-x < N)
                        .mapToObj(x -> color[x][k-x])
                        .toArray(IntVar[]::new)));

        Map<Integer, IntVar[]> rightDiagonal = new HashMap<>();
        IntStream.range(-N+1, N)
                .forEach(k -> rightDiagonal.put(k, IntStream.range(0, N)
                        .filter(x -> 0 <= x-k && x-k < N)
                        .mapToObj(x -> color[x][x-k])
                        .toArray(IntVar[]::new)));

        IntStream.range(0, N)
                .forEach(x -> IntStream.range(0, N)
                        .forEach(y -> {
                            IntVar neighborRowDown = optimizer.newIntVar(0, N-1, neighborRowDownVariable(x, y));
                            IntVar neighborColumnRight = optimizer.newIntVar(0, N-1, neighborColumnRightVariable(x, y));
                            IntVar neighborDiagonalDownLeft = optimizer.newIntVar(0, N-1, neighborDiagonalDownLeftVariable(x, y));
                            IntVar neighborDiagonalDownRight = optimizer.newIntVar(0, N-1, neighborDiagonalDownRightVariable(x, y));

                            IntVar neighborOffsetDown = optimizer.newIntVar(0, N+1, neighborOffsetDownVariable(x, y));
                            IntVar neighborOffsetRight = optimizer.newIntVar(0, N+1, neighborOffsetRightVariable(x, y));
                            IntVar neighborOffsetDownLeft = optimizer.newIntVar(0, N+1, neighborOffsetDownLeftVariable(x, y));
                            IntVar neighborOffsetDownRight = optimizer.newIntVar(0, N+1, neighborOffsetDownRightVariable(x, y));

                            IntVar[] columnNo0Ints = columnNo0.get(y);
                            IntVar[] rowNo0Ints = rowNo0.get(x);
                            IntVar[] diagonalLeftNo0Ints = diagonalLeftNo0.get(x);
                            IntVar[] diagonalRightNo0Ints = diagonalRightNo0.get(y);

                            IntVar[] columnNo0IntsDown = IntStream.range(x+1, N)
                                    .mapToObj(i -> columnNo0Ints[i])
                                    .toArray(IntVar[]::new);
                            IntVar[] rowNo0IntsRight = IntStream.range(y+1, N)
                                    .mapToObj(i -> rowNo0Ints[i])
                                    .toArray(IntVar[]::new);
                            IntVar[] diagonalNo0IntsDownLeft = IntStream.range(x+1, N)
                                    .mapToObj(i -> diagonalLeftNo0Ints[i])
                                    .toArray(IntVar[]::new);
                            IntVar[] diagonalNo0IntsDownRight = IntStream.range(y+1, N)
                                    .mapToObj(i -> diagonalRightNo0Ints[i])
                                    .toArray(IntVar[]::new);

                            cpModel.addMinEquality(neighborRowDown, columnNo0IntsDown);
                            cpModel.addMinEquality(neighborColumnRight, rowNo0IntsRight);
                            cpModel.addMinEquality(neighborDiagonalDownLeft, diagonalNo0IntsDownLeft);
                            cpModel.addMinEquality(neighborDiagonalDownRight, diagonalNo0IntsDownRight);

                            cpModel.addEquality(neighborRowDown, LinearExpr.newBuilder().add(x).add(neighborOffsetDown).build());
                            cpModel.addEquality(neighborColumnRight, LinearExpr.newBuilder().add(y).add(neighborOffsetRight).build());
                            cpModel.addEquality(neighborDiagonalDownLeft, LinearExpr.newBuilder().add(x).add(neighborOffsetDownLeft).build());
                            cpModel.addEquality(neighborDiagonalDownRight, LinearExpr.newBuilder().add(y).add(neighborOffsetDownRight).build());

                            IntVar neighborColorDown = optimizer.newIntVar(-1, C, neighborColorDownVariable(x, y));
                            IntVar neighborColorRight = optimizer.newIntVar(-1, C, neighborColorRightVariable(x, y));
                            IntVar neighborColorDownLeft = optimizer.newIntVar(-1, C, neighborColorDownLeftVariable(x, y));
                            IntVar neighborColorDownRight = optimizer.newIntVar(-1, C, neighborColorDownRightVariable(x, y));

                            // color[x+neighborOffsetDown][y] = neighborColorDown
                            // color[x][y+neighborOffsetRight] = neighborColumnRight
                            // color[x-neighborOffsetDownLeft][y+neighborOffsetDownLeft] = neighborColorDownLeft
                            // color[x+neighborOffsetDownRight][y+neighborOffsetDownRight] = neighborColorDownLeft
                            cpModel.addElement(neighborRowDown, column.get(y), neighborColorDown);
                            cpModel.addElement(neighborColumnRight, row.get(x), neighborColorRight);
                            cpModel.addElement(neighborDiagonalDownLeft, leftDiagonal.get(x+y), neighborColorDownLeft);
                            cpModel.addElement(neighborDiagonalDownRight, rightDiagonal.get(x-y), neighborColorDownRight);

                            BoolVar attackPairsDown = optimizer.newBoolVar(attackPairsDownVariable(x, y));
                            BoolVar attackPairsRight = optimizer.newBoolVar(attackPairsRightVariable(x, y));
                            BoolVar attackPairsDownLeft = optimizer.newBoolVar(attackPairsDownLeftVariable(x, y));
                            BoolVar attackPairsDownRight = optimizer.newBoolVar(attackPairsDownRightVariable(x, y));

                            // attackPairsDown <=> neighborColorDown == c
                            // attackPairsRight <=> neighborColorRight == c
                            // attackPairsDownLeft <=> neighborColorDownLeft == c
                            // attackPairsDownRight <=> neighborColorDownRight == c
                            cpModel.addEquality(neighborColorDown, color[x][y])
                                    .onlyEnforceIf(attackPairsDown);
                            cpModel.addEquality(neighborColorRight, color[x][y])
                                    .onlyEnforceIf(attackPairsRight);
                            cpModel.addEquality(neighborColorDownLeft, color[x][y])
                                    .onlyEnforceIf(attackPairsDownLeft);
                            cpModel.addEquality(neighborColorDownRight, color[x][y])
                                    .onlyEnforceIf(attackPairsDownRight);

                            // attackPairs[x][y] = attackPairsDown + attackPairsRight + attackPairsDownLeft + attackPairsDownRight
                            attackPairs[x][y] = optimizer.newIntVar(0, 4, attackPairsVariable(x, y));
                            cpModel.addEquality(attackPairs[x][y], LinearExpr.sum(new LinearArgument[] {
                                    attackPairsDown,
                                    attackPairsRight,
                                    attackPairsDownLeft,
                                    attackPairsDownRight
                            }));
                        }));

        int moveOffsetSqrtMax = intSqrt(N-1);

        IntStream.range(0, T)
                .forEach(t -> {
                    moveOffsets[t] = optimizer.newIntVar(0, N-1, moveOffsetVariable(t));
                    moveCosts[t] = optimizer.newIntVar(0, moveOffsetSqrtMax, moveCostVariable(t));

                    // approximate moveCosts[t] as the square root of moveOffsets[t]
                    // SR2 = SR^2 <= n < (SR+1)^2

                    IntVar moveOffsetSqrt2 = optimizer.newIntVar(0, N-1, moveOffsetSqrt2Variable(t));

                    // moveOffsetSqrt2 = moveCosts[t] ^ 2
                    cpModel.addMultiplicationEquality(moveOffsetSqrt2, moveCosts[t], moveCosts[t]);
                    // moveOffsetSqrt2 <= moveOffsets[t]
                    cpModel.addLessOrEqual(moveOffsetSqrt2, moveOffsets[t]);
                    // moveOffsetSqrt2 + 2 moveCosts[t] + 1 > moveOffsets[t]
                    cpModel.addGreaterThan(LinearExpr.newBuilder()
                            .add(moveOffsetSqrt2)
                            .addTerm(moveCosts[t], 2)
                            .add(1)
                            .build(), moveOffsets[t]);
                });
    }

    @Override
    public void buildConstraints() {

    }

    @Override
    public void buildCost() {
        IntVar[] attackPairInts = Arrays.stream(attackPairs)
                .flatMap(Arrays::stream)
                .toArray(IntVar[]::new);

        LinearExpr costExpression = LinearExpr.newBuilder()
                .addTerm(LinearExpr.sum(attackPairInts), N)
                .add(LinearExpr.sum(moveCosts))
                .build();

        IntVar cost = optimizer.newIntVar(0, maxCost(), optimizer.costVariable());
        cpModel.addEquality(cost, costExpression);

        int minCost = optimizer.optimizeCost();

        System.err.println(String.format("min cost %d", minCost));
    }

    private int maxCost() {
        return N * maxPairs();
    }

    private int maxPairs() {
        return N*(N+1) / 2;
    }

    /**
     *
     * @param n an integer
     * @return an integer approximation of Square Root
     */
    private int intSqrt(int n) {
        return (int) Math.sqrt(n);
    }
}
