package com.serafinebot.dint.game_2048;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.serafinebot.dint.game_2048.data.Score;
import com.serafinebot.dint.game_2048.data.ScoreHelper;
import com.serafinebot.dint.game_2048.touch.OnSwipeListener;
import com.serafinebot.dint.game_2048.touch.OnSwipeListenerDelegate;
import com.serafinebot.dint.game_2048.touch.SwipeDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameActivity extends AppCompatActivity implements OnSwipeListenerDelegate {
    private static final int GRID_WIDTH = 4;
    private static final int GRID_HEIGHT = 4;
    private static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;
    private static final int START_PERCENT_SMALL = 70;
    private static final String GRID_KEY = "grid";
    private static final String PREVIOUS_KEY = "previous";
    private int[] grid = new int[GRID_SIZE];
    private final int[] sum = new int[GRID_SIZE];
    private int[] previous = null;

    private final ScoreHelper scoreHelper = new ScoreHelper(this);
    private final List<TextView> cells = new ArrayList<>();
    private TextView score_number = null;
    private TextView best_number = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_layout);

        this.score_number = findViewById(R.id.score_number);
        this.best_number = findViewById(R.id.best_number);
        this.best_number.setText(String.valueOf(this.scoreHelper.getHighest().score));

        GridLayout gameLayout = findViewById(R.id.game_grid);
        new OnSwipeListener(this, this, gameLayout);
        for (int i = 0; i < gameLayout.getChildCount(); i++) {
            View child = gameLayout.getChildAt(i);
            if (child instanceof TextView) {
                TextView cell = (TextView) child;
                this.cells.add(cell);
            }
        }
        if (savedInstanceState != null) {
            this.grid = savedInstanceState.getIntArray(GRID_KEY);
            this.previous = savedInstanceState.getIntArray(PREVIOUS_KEY);
        } else {
            addRandom();
        }
        updateGrid();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putIntArray(GRID_KEY, this.grid);
        outState.putIntArray(PREVIOUS_KEY, this.previous);
        super.onSaveInstanceState(outState);
    }

    public void savePressed(@NonNull View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter player name:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            Score score = new Score();
            score.score = getScore();
            score.player = input.getText().toString();
            String message = this.scoreHelper.add(score) < 0 ? "Could not save score" : "Score saved";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
        builder.setNegativeButton("Cancel", ((dialog, which) -> {
            dialog.cancel();
        }));
        builder.show();
    }

    public void resetPressed(@NonNull View view) {
        for (int i = 0; i < this.grid.length; i++) {
            this.grid[i] = 0;
            this.sum[i] = 0;
        }
        this.previous = null;
        addRandom();
        updateGrid();
    }

    public void undoPressed(@NonNull View view) {
        if (this.previous != null) {
            this.grid = this.previous.clone();
            this.previous = null;
            updateGrid();
        } else {
            Toast.makeText(this, "Unable to undo", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isFilled() {
        for (int i = 0; i < GRID_SIZE; i++) if (this.grid[i] == 0) return false;
        return true;
    }

    public void addRandom() {
        if (isFilled()) return;
        int random = (int) (Math.random() * GRID_SIZE);
        for (int i = random; i < GRID_SIZE + random; i++) {
            int index = i % GRID_SIZE;
            int val = this.grid[index];
            if (val == 0) {
                int chance = (int) (Math.random() * 100);
                int num = chance <= START_PERCENT_SMALL ? 2 : 4;
                this.grid[index] = num;
                break;
            }
        }
    }

    public int getScore() {
        int score = 0;
        for (int value : this.grid) score += value;
        return score;
    }

    public void updateGrid() {
        for (int i = 0; i < this.cells.size(); i++) {
            TextView cell = this.cells.get(i);
            int val = this.grid[i];
            String newText = "";
            int color = R.color.game_color_pale;
            if (val != 0) {
                newText = String.valueOf(val);
                color = R.color.game_color_light;
            }
            cell.setText(newText);
            cell.setBackgroundTintList(getColorStateList(color));
        }
        this.score_number.setText(String.valueOf(getScore()));
    }

    public boolean inBounds(int val, int x, int y) {
        return val >= x && val <= y;
    }

    public int getVal(int x, int y) {
        return getVal(x, y, this.grid);
    }

    public int getVal(int x, int y, int[] matrix) {
        if (!inBounds(x, 0, GRID_WIDTH - 1) || !inBounds(y, 0, GRID_HEIGHT - 1)) return 0;
        return matrix[(y * GRID_WIDTH) + x];
    }

    public void setVal(int x, int y, int val) {
        setVal(x, y, this.grid, val);
    }

    public void setVal(int x, int y, int[] matrix, int val) {
        if (!inBounds(x, 0, GRID_WIDTH - 1) || !inBounds(y, 0, GRID_HEIGHT - 1)) return;
        matrix[(y * GRID_WIDTH) + x] = val;
    }

    public void move(int srcX, int srcY, int dstX, int dstY) {
        if (!inBounds(srcX, 0, GRID_WIDTH - 1) || !inBounds(srcY, 0, GRID_HEIGHT - 1) ||
                !inBounds(srcX, 0, GRID_WIDTH - 1) || !inBounds(srcY, 0, GRID_HEIGHT - 1)) return;
        int val = getVal(srcX, srcY);
        setVal(dstX, dstY, val);
        setVal(srcX, srcY, 0);
    }

    public void move(int[] grid, int srcX, int srcY, int dstX, int dstY) {
        if (!inBounds(srcX, 0, GRID_WIDTH - 1) || !inBounds(srcY, 0, GRID_HEIGHT - 1) ||
                !inBounds(srcX, 0, GRID_WIDTH - 1) || !inBounds(srcY, 0, GRID_HEIGHT - 1)) return;
        int val = getVal(srcX, srcY, grid);
        setVal(dstX, dstY, grid, val);
        setVal(srcX, srcY, grid, 0);
    }

    public void moveValue(int x, int y, final int xInc, final int yInc, int[] grid, int[] sumArr) {
        int nextX = x;
        int nextY = y;
        do {
            nextX += xInc;
            nextY += yInc;
            if (!(inBounds(nextX, 0, GRID_WIDTH - 1) && inBounds(nextY, 0, GRID_HEIGHT - 1))) break;
            int val = getVal(x, y, grid);
            int nextSum = getVal(nextX, nextY, sumArr);
            int next = getVal(nextX, nextY, grid);
            if (next == 0) {
                move(grid, x, y, nextX, nextY);
                x = nextX;
                y = nextY;
            } else if (next == val && nextSum == 0) {
                int sum = next + val;
                setVal(nextX, nextY, grid, sum);
                setVal(nextX, nextY, sumArr, 1);
                setVal(x, y, grid, 0);
            } else {
                break;
            }
        } while (inBounds(nextX, 0, GRID_WIDTH - 1) && inBounds(nextY, 0, GRID_HEIGHT - 1));
    }

    public boolean getCondition(int add, int current, int end) {
        return add > 0 ? current <= end : current >= end;
    }

    public boolean equal(int[] v1, int[] v2) {
        if (v1 == null || v2 == null) return false;
        if (v1.length != v2.length) return false;
        for (int i = 0; i < v1.length; i++)
            if (v1[i] != v2[i]) return false;
        return true;
    }

    public void makeMove(SwipeDirection direction, int[] grid, int[] sum) {
        int xinc = 0;
        int yinc = 0;
        int startX = 0;
        int startY = 0;
        int endX = GRID_WIDTH;
        int endY = GRID_HEIGHT;
        switch (direction) {
            case LEFT:
                startX = 0;
                endX = GRID_WIDTH - 1;
                xinc = -1;
                break;
            case RIGHT:
                startX = GRID_WIDTH - 1;
                endX = 0;
                xinc = 1;
                break;
            case TOP:
                startY = 0;
                endY = GRID_HEIGHT - 1;
                yinc = -1;
                break;
            case BOTTOM:
                yinc = 1;
                startY = GRID_HEIGHT - 1;
                endY = 0;
                break;
        }
        int xadd = 1;
        int yadd = 1;
        if (Math.min(startX, endX) == endX) xadd = -1;
        if (Math.min(startY, endY) == endY) yadd = -1;
        for (int y = startY; getCondition(yadd, y, endY); y += yadd) {
            for (int x = startX; getCondition(xadd, x, endX); x += xadd) {
                int val = getVal(x, y, grid);
                if (val == 0) continue;
                moveValue(x, y, xinc, yinc, grid, sum);
            }
        }
    }

    public boolean checkGameOver() {
        for (SwipeDirection direction : SwipeDirection.values()) {
            int[] grid = Arrays.copyOf(this.grid, this.grid.length);
            int[] tmp = Arrays.copyOf(this.grid, this.grid.length);
            int[] sum = new int[this.grid.length];
            makeMove(direction, grid, sum);
            if (!equal(grid, tmp)) return false;
        }
        return true;
    }

    @Override
    public void didSwipe(SwipeDirection direction) {
        int[] tmp = this.grid.clone();
        Arrays.fill(sum, 0);
        makeMove(direction, this.grid, this.sum);
        if (!equal(tmp, this.grid)) {
            this.previous = tmp;
            addRandom();
        }
        updateGrid();
        boolean gameOver = checkGameOver();
        if (gameOver) {
            new AlertDialog.Builder(this)
                    .setTitle("Game Over")
                    .setMessage("There are no possible movements left...")
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .setNegativeButton("RESTART", (dialog, which) -> {
                        resetPressed(null);
                    })
                    .show();
            this.previous = null;
        }
    }
}
