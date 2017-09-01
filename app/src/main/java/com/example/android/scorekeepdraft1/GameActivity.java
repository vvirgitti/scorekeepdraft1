/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.android.scorekeepdraft1;

import android.content.ClipData;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.scorekeepdraft1.undoredo.BaseLog;
import com.example.android.scorekeepdraft1.undoredo.GameHistory;
import com.example.android.scorekeepdraft1.undoredo.GameLog;
import com.example.android.scorekeepdraft1.undoredo.RunsLog;

import com.example.android.scorekeepdraft1.data.PlayerStatsContract.PlayerStatsEntry;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static android.R.attr.action;
import static com.example.android.scorekeepdraft1.R.id.reset;

/**
 * @author Eddie
 */
public class GameActivity extends AppCompatActivity /*implements LoaderManager.LoaderCallbacks<Cursor>*/ {

    private Cursor mCursor;

    private TextView scoreboard;
    private TextView nowBatting;
    private TextView outsDisplay;
    private TextView inningDisplay;
    private TextView avgDisplay;
    private TextView rbiDisplay;
    private TextView runDisplay;
    private TextView hrDisplay;

    private Button submitPlay;
    private Button resetBases;

    private RadioGroup group1;
    private RadioGroup group2;
    private String result;

    private ImageView batterDisplay;
    private ImageView outTrash;
    private TextView firstDisplay;
    private TextView secondDisplay;
    private TextView thirdDisplay;
    private TextView homeDisplay;

    //TODO: CREATE A SELECT TEAMS AND SET LINEUP ACTIVITY (AND CARRY CHOICES OVER TO GAME ACTIVITY)
    //TODO: ADD LEADERBOARD DISPLAYS ACTIVITY (ALL ONE ACTIVITY USING "SORTBY"?)
    //TODO: ADD PLAYER STATS DISPLAYS BY TEAM (ALL ONE ACTIVITY USING "WHERE"?)
    //TODO: ADD RUNS/WINS/ETC TO TEAM STATS AFTER GAME IS OVER
    //TODO: FIGURE OUT HOW TO END GAME AND NEXT STEPS


    //private boolean valuesAdded = false;
    //temporary default values
    private String[] listOfPlayers = {"a1", "b1", "a2", "b2", "a3", "b3", "a4", "b4", "a5", "b5", "a6", "b6"};
    private Team awayTeam = new Team("Isotopes");
    private Team homeTeam = new Team("Purptopes");

    //temporary buttonw?
    private Button undoButton;
    private Button redoButton;

    private String tempBatter;
    private boolean inningChanged = false;

    private int inningNumber = 1;
    private int gameOuts = 0;
    private int tempOuts;
    private int tempRuns;

    private Player currentBatter;
    private Team currentTeam;

    private NumberFormat formatter = new DecimalFormat("#.000");
    private BaseLog currentBaseLogStart;
    private BaseLog currentBaseLogEnd;
    private RunsLog currentRunsLog;

    private GameHistory gameHistory;
    private int gameLogIndex = 0;

    private boolean undoRedo = false;

    private boolean playEntered = false;
    private boolean batterMoved = false;
    private boolean firstOccupied = false;
    private boolean secondOccupied = false;
    private boolean thirdOccupied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        scoreboard = (TextView) findViewById(R.id.scoreboard);
        nowBatting = (TextView) findViewById(R.id.nowbatting);
        outsDisplay = (TextView) findViewById(R.id.num_of_outs);
        inningDisplay = (TextView) findViewById(R.id.inning);
        avgDisplay = (TextView) findViewById(R.id.avgdisplay);
        rbiDisplay = (TextView) findViewById(R.id.rbidisplay);
        runDisplay = (TextView) findViewById(R.id.rundisplay);
        hrDisplay = (TextView) findViewById(R.id.hrdisplay);

        group1 = (RadioGroup) findViewById(R.id.group1);
        group2 = (RadioGroup) findViewById(R.id.group2);

        submitPlay = (Button) findViewById(R.id.submit);
        submitPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit();
            }
        });
        submitPlay.setVisibility(View.INVISIBLE);

        resetBases = (Button) findViewById(reset);
        resetBases.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetBases(currentBaseLogStart);
            }
        });
        resetBases.setVisibility(View.INVISIBLE);

        //temporary?
        undoButton = (Button) findViewById(R.id.undobutton);
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoPlay();
            }
        });
        redoButton = (Button) findViewById(R.id.redobutton);
        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redoPlay();
            }
        });
        redoButton.setVisibility(View.INVISIBLE);

        batterDisplay = (ImageView) findViewById(R.id.batter);
        firstDisplay = (TextView) findViewById(R.id.first_display);
        secondDisplay = (TextView) findViewById(R.id.second_display);
        thirdDisplay = (TextView) findViewById(R.id.third_display);
        homeDisplay = (TextView) findViewById(R.id.home_display);
        outTrash = (ImageView) findViewById(R.id.trash);

        batterDisplay.setOnLongClickListener(new MyClickListener());

        firstDisplay.setOnDragListener(new MyDragListener());
        secondDisplay.setOnDragListener(new MyDragListener());
        thirdDisplay.setOnDragListener(new MyDragListener());
        homeDisplay.setOnDragListener(new MyDragListener());
        outTrash.setOnDragListener(new MyDragListener());

        startGame();
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        playEntered = true;
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.single:
                if (checked)
                    result = "1b";
                    group2.clearCheck();
                    break;
            case R.id.dbl:
                if (checked)
                    result = "2b";
                    group2.clearCheck();
                    break;
            case R.id.triple:
                if (checked)
                    result = "3b";
                    group2.clearCheck();
                    break;
            case R.id.hr:
                if (checked)
                    result = "hr";
                    group2.clearCheck();
                    break;
            case R.id.bb:
                if (checked)
                    result = "bb";
                    group1.clearCheck();
                    break;
            case R.id.out:
                if (checked)
                    result = "out";
                    group1.clearCheck();
                    break;
            case R.id.error:
                if (checked)
                    result = "out";
                    group1.clearCheck();
                    break;
            case R.id.fc:
                if (checked)
                    result = "out";
                    group1.clearCheck();
                    break;
            case R.id.sf:
                if (checked)
                    result = "sf";
                    group1.clearCheck();
                    break;
        }
        if(batterMoved) {
            submitPlay.setVisibility(View.VISIBLE);
        }
    }

    public void setBaseListeners() {
        if (firstDisplay.getText().toString().isEmpty()) {
            firstDisplay.setOnLongClickListener(null);
            firstDisplay.setOnDragListener(new MyDragListener());
            firstOccupied = false;
        } else {
            firstDisplay.setOnLongClickListener(new MyClickListener());
            firstDisplay.setOnDragListener(null);
            firstOccupied = true;
        }

        if (secondDisplay.getText().toString().isEmpty()) {
            secondDisplay.setOnLongClickListener(null);
            secondDisplay.setOnDragListener(new MyDragListener());
            secondOccupied = false;
        } else {
            secondDisplay.setOnLongClickListener(new MyClickListener());
            secondDisplay.setOnDragListener(null);
            secondOccupied = true;
        }

        if (thirdDisplay.getText().toString().isEmpty()) {
            thirdDisplay.setOnLongClickListener(null);
            thirdDisplay.setOnDragListener(new MyDragListener());
            thirdOccupied = false;
        } else {
            thirdDisplay.setOnLongClickListener(new MyClickListener());
            thirdDisplay.setOnDragListener(null);
            thirdOccupied = true;
        }
        homeDisplay.setOnDragListener(new MyDragListener());
    }

    public void startGame() {
        //TEMPORARY
        for (int i = 0; i < listOfPlayers.length; i++) {
            if (i % 2 == 0) {
                awayTeam.addPlayer(new Player(listOfPlayers[i]));
            } else {
                homeTeam.addPlayer(new Player(listOfPlayers[i]));
            }
        }
        awayTeam.setCurrentRuns(0);
        homeTeam.setCurrentRuns(0);

        gameHistory = new GameHistory();
        //TODO? this.manager = new UndoRedoManager(this);
        currentTeam = awayTeam;
        currentBatter = awayTeam.getLineup().get(0);
        currentRunsLog = new RunsLog();
        currentBaseLogStart = new BaseLog(currentTeam, currentBatter, "", "", "", 0, 0, 0);
        gameHistory.addGameLog(new GameLog(currentBaseLogStart, currentRunsLog, "start", null, false));
        scoreboard.setText(awayTeam.getName() + " " + awayTeam.getCurrentRuns() + "    " + homeTeam.getName() + " " + homeTeam.getCurrentRuns());

        try {
            startCursor();
            setDisplays();
        } catch (Exception e) {
            Toast.makeText(GameActivity.this, "Adding a temporary database first!", Toast.LENGTH_LONG).show();
            for (String player : listOfPlayers) {
                ContentValues values = new ContentValues();
                values.put(PlayerStatsEntry.COLUMN_NAME, player);
                values.put(PlayerStatsEntry.COLUMN_TEAM, "xxx");
                values.put(PlayerStatsEntry.COLUMN_1B, 0);
                values.put(PlayerStatsEntry.COLUMN_2B, 0);
                values.put(PlayerStatsEntry.COLUMN_3B, 0);
                values.put(PlayerStatsEntry.COLUMN_HR, 0);
                values.put(PlayerStatsEntry.COLUMN_BB, 0);
                values.put(PlayerStatsEntry.COLUMN_SF, 0);
                values.put(PlayerStatsEntry.COLUMN_OUT, 0);
                values.put(PlayerStatsEntry.COLUMN_RUN, 0);
                values.put(PlayerStatsEntry.COLUMN_RBI, 0);
                getContentResolver().insert(PlayerStatsEntry.CONTENT_URI1, values);
            }
            startCursor();
            setDisplays();
        }
    }

    public void nextBatter() {
        if (gameOuts >= 3) {
            nextInning();
        } else {
            currentTeam.increaseIndex();
        }
        String previousBatterName = currentBatter.getName();
        currentBatter = currentTeam.getPlayer(currentTeam.getIndex());

        currentBaseLogEnd = new BaseLog(currentTeam, currentBatter, firstDisplay.getText().toString(),
                secondDisplay.getText().toString(), thirdDisplay.getText().toString(),
                gameOuts, awayTeam.getCurrentRuns(), homeTeam.getCurrentRuns());
        currentBaseLogStart = new BaseLog(currentTeam, currentBatter, firstDisplay.getText().toString(),
                secondDisplay.getText().toString(), thirdDisplay.getText().toString(),
                gameOuts, awayTeam.getCurrentRuns(), homeTeam.getCurrentRuns());
        gameHistory.addGameLog(new GameLog(currentBaseLogEnd, currentRunsLog, result, previousBatterName, inningChanged));
        gameLogIndex++;

        startCursor();
        setDisplays();
        group1.clearCheck();
        group2.clearCheck();
        currentRunsLog = new RunsLog();
        submitPlay.setVisibility(View.INVISIBLE);
        resetBases.setVisibility(View.INVISIBLE);
        tempOuts = 0;
        tempRuns = 0;
        playEntered = false;
        batterMoved = false;
        inningChanged = false;
    }

    public void nextInning() {
        gameOuts = 0;
        emptyBases();
        if (inningNumber >= 6) {
            Toast.makeText(GameActivity.this, "GAME OVER", Toast.LENGTH_LONG).show();
        }
        currentTeam.increaseIndex();
        if (currentTeam.getIndex() >= currentTeam.getLineup().size()) {
            currentTeam.setIndex(0);
        }
        if (currentTeam == awayTeam) {
            currentTeam = homeTeam;
        } else {
            currentTeam = awayTeam;
            inningNumber++;
            inningDisplay.setText(String.valueOf(inningNumber));
        }
        inningChanged = true;
    }

    public void startCursor() {
        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {currentBatter.getName()};
        mCursor = getContentResolver().query(
                PlayerStatsEntry.CONTENT_URI1, null,
                selection, selectionArgs, null
        );
        mCursor.moveToNext();
    }

    //sets the textview displays with updated player/game data
    public void setDisplays() {
        batterDisplay.setVisibility(View.VISIBLE);

        int nameIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_NAME);
        int hrIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_HR);
        int rbiIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RBI);
        int runIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RUN);
        int singleIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_1B);
        int doubleIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_2B);
        int tripleIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_3B);
        int outIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_OUT);

        String name = mCursor.getString(nameIndex);
        int displayHR = mCursor.getInt(hrIndex);
        int displayRBI = mCursor.getInt(rbiIndex);
        int displayRun = mCursor.getInt(runIndex);
        int singles = mCursor.getInt(singleIndex);
        int doubles = mCursor.getInt(doubleIndex);
        int triples = mCursor.getInt(tripleIndex);
        int playerOuts = mCursor.getInt(outIndex);

        double avg = calculateAverage(singles, doubles, triples, displayHR, playerOuts, 0);

        nowBatting.setText("Now batting: " + currentBatter + " (" + name);
        avgDisplay.setText("AVG: " + formatter.format(avg));
        hrDisplay.setText("HR: " + displayHR);
        rbiDisplay.setText("RBI: " + displayRBI);
        runDisplay.setText("R: " + displayRun);

        scoreboard.setText(awayTeam.getName() + " " + awayTeam.getCurrentRuns() + "    " + homeTeam.getName() + " " + homeTeam.getCurrentRuns());
    }

    public void updatePlayerStats(String action) {
        String[] selectionArgs;
        if(undoRedo) {
            String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
            selectionArgs = new String[]{tempBatter};
            mCursor = getContentResolver().query(
                    PlayerStatsEntry.CONTENT_URI1, null,
                    selection, selectionArgs, null
            );
        } else {
            selectionArgs = new String[]{currentBatter.getName()};
        }
        mCursor.moveToFirst();
        ContentValues values = new ContentValues();
        int valueIndex;
        int newValue;

        switch (action) {
            case "1b":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_1B);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_1B, newValue);
                break;
            case "2b":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_2B);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_2B, newValue);
                break;
            case "3b":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_3B);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_3B, newValue);
                break;
            case "hr":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_HR);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_HR, newValue);
                break;
            case "bb":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_BB);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_BB, newValue);
                break;
            case "sf":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_SF);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_SF, newValue);
                break;
            case "out":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_OUT);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_OUT, newValue);
                break;
            default:
                Toast.makeText(GameActivity.this, "SOMETHING FUCKED UP BIG TIME!!!", Toast.LENGTH_LONG).show();
                break;
        }

        int rbiCount = currentRunsLog.getRBICount();
        if (rbiCount > 0) {
            valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RBI);
            newValue = mCursor.getInt(valueIndex) + rbiCount;
            values.put(PlayerStatsEntry.COLUMN_RBI, newValue);
        }

        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        getContentResolver().update(
                PlayerStatsEntry.CONTENT_URI1,
                values,
                selection,
                selectionArgs
        );

        if(rbiCount > 0) {
            for(String player : currentRunsLog.getPlayersScored()) {
                addRun(player);
            }
        }
        startCursor();
        setDisplays();
    }

    public void undoPlayerStats(String action) {
        if (tempBatter == null) {
            Toast.makeText(GameActivity.this, "TWAS NULL!!!", Toast.LENGTH_SHORT).show();
            return;
        }
        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {tempBatter};
        mCursor = getContentResolver().query(
                PlayerStatsEntry.CONTENT_URI1, null,
                selection, selectionArgs, null);
        mCursor.moveToFirst();
        ContentValues values = new ContentValues();
        int valueIndex;
        int newValue;

        switch (action) {
            case "1b":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_1B);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_1B, newValue);
                break;
            case "2b":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_2B);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_2B, newValue);
                break;
            case "3b":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_3B);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_3B, newValue);
                break;
            case "hr":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_HR);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_HR, newValue);
                break;
            case "bb":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_BB);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_BB, newValue);
                break;
            case "sf":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_SF);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_SF, newValue);
                break;
            case "out":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_OUT);
                newValue = mCursor.getInt(valueIndex) - 1;
                values.put(PlayerStatsEntry.COLUMN_OUT, newValue);
                break;
            default:
                Toast.makeText(GameActivity.this, "SOMETHING FUCKED UP BIG TIME!!!", Toast.LENGTH_LONG).show();
                break;
        }

        int rbiCount = currentRunsLog.getRBICount();
        if (rbiCount > 0) {
            valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RBI);
            newValue = mCursor.getInt(valueIndex) - rbiCount;
            values.put(PlayerStatsEntry.COLUMN_RBI, newValue);
        }

/*        String selection = PlayervStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {currentBatter.getName()};*/
        getContentResolver().update(
                PlayerStatsEntry.CONTENT_URI1,
                values,
                selection,
                selectionArgs
        );

        if(rbiCount > 0) {
            for(String player : currentRunsLog.getPlayersScored()) {
                subtractRun(player);
            }
        }
        startCursor();
        setDisplays();
    }

    public void addRun(String player) {
        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {player};
        mCursor = getContentResolver().query(
                PlayerStatsEntry.CONTENT_URI1, null,
                selection, selectionArgs, null
        );
        mCursor.moveToNext();

        ContentValues values = new ContentValues();
        int valueIndex;
        int newValue;
        valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RUN);
        newValue = mCursor.getInt(valueIndex) + 1;
        values.put(PlayerStatsEntry.COLUMN_RUN, newValue);

        getContentResolver().update(
                PlayerStatsEntry.CONTENT_URI1,
                values,
                selection,
                selectionArgs
        );
        if (!undoRedo) {
            currentTeam.addRun();
        }
    }

    public void subtractRun(String player) {
        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {player};
        mCursor = getContentResolver().query(
                PlayerStatsEntry.CONTENT_URI1, null,
                selection, selectionArgs, null
        );
        mCursor.moveToNext();

        ContentValues values = new ContentValues();
        int valueIndex;
        int newValue;
        valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RUN);
        newValue = mCursor.getInt(valueIndex) - 1;
        values.put(PlayerStatsEntry.COLUMN_RUN, newValue);

        getContentResolver().update(
                PlayerStatsEntry.CONTENT_URI1,
                values,
                selection,
                selectionArgs
        );
    }

    public void resetBases(BaseLog baseLog) {
        String[] bases = baseLog.getBasepositions();
        firstDisplay.setText(bases[0]);
        secondDisplay.setText(bases[1]);
        thirdDisplay.setText(bases[2]);
        batterDisplay.setVisibility(View.VISIBLE);
        batterMoved = false;
        if(!undoRedo) {
            currentRunsLog.resetPlayersScored();
        }
        submitPlay.setVisibility(View.INVISIBLE);
        resetBases.setVisibility(View.INVISIBLE);
        setBaseListeners();
        tempOuts = 0;
        tempRuns = 0;
        outsDisplay.setText(gameOuts + "outs");
        scoreboard.setText(awayTeam.getName() + " " + awayTeam.getCurrentRuns() + "    " + homeTeam.getName() + " " + homeTeam.getCurrentRuns());
    }

    public void emptyBases() {
        firstDisplay.setText("");
        secondDisplay.setText("");
        thirdDisplay.setText("");
    }

    public double calculateAverage(int singles, int doubles, int triples, int hrs, int outs, int errors) {
        double hits = (double) (singles + doubles + triples + hrs);
        return (hits / (outs + errors + hits));
    }

    public void onSubmit() {
        if(undoRedo) {
            gameHistory.updateGameLog(gameLogIndex);
            undoRedo = false;
        }
        updatePlayerStats(result);
        gameOuts += tempOuts;
        nextBatter();
        outsDisplay.setText(gameOuts + "outs");
    }

    public void undoPlay() {
        String undoResult;
        if(gameLogIndex > 0) {
            undoRedo = true;
            GameLog gameLog1 = gameHistory.getGameLog(gameLogIndex);
            tempBatter = gameLog1.getPreviousBatter();
            undoResult = gameLog1.getResult();
            currentRunsLog = gameLog1.getRunsLog();

            gameLogIndex--;
            redoButton.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(GameActivity.this, "This is the beginning of the game!", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(GameActivity.this, "gameindex =" + gameLogIndex, Toast.LENGTH_SHORT).show();
        GameLog gameLog2 = gameHistory.getGameLog(gameLogIndex);
        BaseLog baseLogEnd = gameLog2.getBaseLogEnd();
        currentBaseLogStart = baseLogEnd;
        //undoRuns
        awayTeam.setCurrentRuns(baseLogEnd.getAwayTeamRuns());
        homeTeam.setCurrentRuns(baseLogEnd.getHomeTeamRuns());

        gameOuts = baseLogEnd.getOutCount();
        currentTeam = baseLogEnd.getTeam();
        currentBatter = baseLogEnd.getBatter();
        currentTeam.setIndex(currentBatter);
        resetBases(baseLogEnd);
        undoPlayerStats(undoResult);
        setDisplays();
    }

    public void redoPlay() {
        if(gameLogIndex < gameHistory.getAllGameLogs().size() -1) {
            undoRedo = true;
            gameLogIndex++;
        }  else {
            redoButton.setVisibility(View.INVISIBLE);
            Toast.makeText(GameActivity.this, "Already caught up! \n gameindex =" + gameLogIndex, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(GameActivity.this, "gameindex =" + gameLogIndex, Toast.LENGTH_SHORT).show();
        GameLog gameLog = gameHistory.getGameLog(gameLogIndex);
        currentRunsLog = gameLog.getRunsLog();
        BaseLog baseLogEnd = gameLog.getBaseLogEnd();
        currentBaseLogStart = baseLogEnd;
        awayTeam.setCurrentRuns(baseLogEnd.getAwayTeamRuns());
        homeTeam.setCurrentRuns(baseLogEnd.getHomeTeamRuns());
        gameOuts = baseLogEnd.getOutCount();
        currentTeam = baseLogEnd.getTeam();
        currentBatter = baseLogEnd.getBatter();
        tempBatter = gameLog.getPreviousBatter();
        String redoResult = gameLog.getResult();
        if(gameLog.isInningChanged()) {
            if(currentTeam == awayTeam) {
                homeTeam.increaseIndex();
            } else if (currentTeam == homeTeam){
                awayTeam.increaseIndex();
            } else {
                Toast.makeText(GameActivity.this, "inningChanged logic is fucked up!!!", Toast.LENGTH_SHORT).show();
            }
            inningChanged = false;
        }
        currentTeam.setIndex(currentBatter);
        resetBases(baseLogEnd);
        updatePlayerStats(redoResult);
        setDisplays();
    }

    public void doPlay() {
        GameLog currentGameLog = gameHistory.getGameLog(gameLogIndex);
        currentRunsLog = currentGameLog.getRunsLog();
        //currentBaseLogStart = currentGameLog.getBaseLogStart();
        awayTeam.setCurrentRuns(currentBaseLogStart.getAwayTeamRuns());
        homeTeam.setCurrentRuns(currentBaseLogStart.getHomeTeamRuns());
        gameOuts = currentBaseLogStart.getOutCount();
        currentTeam = currentBaseLogStart.getTeam();
        currentBatter = currentBaseLogStart.getBatter();
        currentTeam.setIndex(currentBatter);
        startCursor();
        resetBases(currentBaseLogEnd);
        setDisplays();
        nextBatter();
    }

    class MyDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            int action = event.getAction();
            TextView dropPoint = null;
            if (v.getId() != R.id.trash) {
                dropPoint = (TextView) v;
            }
            View eventView = (View) event.getLocalState();

            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        v.setBackgroundColor(Color.LTGRAY);
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(Color.TRANSPARENT);
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    break;
                case DragEvent.ACTION_DROP:
                    String movedPlayer = "";
                    //check later whether can shorten this section
                    if (v.getId() == R.id.trash) {
                        if (eventView instanceof TextView) {
                            TextView draggedView = (TextView) eventView;
                            draggedView.setText("");
                        } else {
                            batterDisplay.setVisibility(View.INVISIBLE);
                            batterMoved = true;
                            if(playEntered) {
                                submitPlay.setVisibility(View.VISIBLE);
                            }
                        }
                        tempOuts++;
                        outsDisplay.setText((gameOuts + tempOuts) + "outs");
                    } else {
                        if (eventView instanceof TextView) {
                            TextView draggedView = (TextView) eventView;
                            movedPlayer = draggedView.getText().toString();
                            dropPoint.setText(movedPlayer);
                            draggedView.setText("");
                        } else {
                            dropPoint.setText(currentBatter.getName());
                            batterDisplay.setVisibility(View.INVISIBLE);
                            batterMoved = true;
                            if(playEntered) {
                                submitPlay.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    v.setBackgroundColor(Color.TRANSPARENT);
                    if (dropPoint == homeDisplay) {
                        if (eventView instanceof TextView) {
                            currentRunsLog.addPlayerScored(movedPlayer);
                        } else {
                            currentRunsLog.addPlayerScored(currentBatter.getName());
                        }
                        homeDisplay.setText("");
                        tempRuns++;
                        if (currentTeam == awayTeam) {
                            scoreboard.setText(awayTeam.getName() + " " + (awayTeam.getCurrentRuns() + tempRuns) + "    " + homeTeam.getName() + " " + (homeTeam.getCurrentRuns()));
                        } else {
                            scoreboard.setText(awayTeam.getName() + " " + (awayTeam.getCurrentRuns()) + "    " + homeTeam.getName() + " " + (homeTeam.getCurrentRuns() + tempRuns));
                        }
                    }
                    resetBases.setVisibility(View.VISIBLE);
                    setBaseListeners();
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(Color.TRANSPARENT);
                    if (eventView instanceof TextView) {
                        eventView.setBackgroundColor(Color.TRANSPARENT);
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    //TODO: LOOK into performance issues of making new onclick/ondrag listeners each time as opposed to perhaps recycling the same one?
    // This defines your touch listener
    private final class MyClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            setBaseListeners();

            switch (view.getId()) {
                case R.id.batter:
                    if (firstOccupied) {
                        secondDisplay.setOnDragListener(null);
                        thirdDisplay.setOnDragListener(null);
                        homeDisplay.setOnDragListener(null);
                    } else if (secondOccupied) {
                        thirdDisplay.setOnDragListener(null);
                        homeDisplay.setOnDragListener(null);
                    } else if (thirdOccupied) {
                        homeDisplay.setOnDragListener(null);
                    }
                    break;
                case R.id.first_display:
                    if (secondOccupied) {
                        thirdDisplay.setOnDragListener(null);
                        homeDisplay.setOnDragListener(null);
                    } else if (thirdOccupied) {
                        homeDisplay.setOnDragListener(null);
                    }
                    break;
                case R.id.second_display:
                    firstDisplay.setOnDragListener(null);
                    if (thirdOccupied) {
                        homeDisplay.setOnDragListener(null);
                    }
                    break;
                case R.id.third_display:
                    firstDisplay.setOnDragListener(null);
                    secondDisplay.setOnDragListener(null);
                    break;
                default:
                    Toast.makeText(GameActivity.this, "SOMETHING WENT WRONG WITH THE SWITCH", Toast.LENGTH_LONG).show();
                    break;
            }

            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.startDragAndDrop(data, shadowBuilder, view, 0);
            } else {
                view.startDrag(data, shadowBuilder, view, 0);
            }
            return true;
        }
    }

    public void subtractOut() {
        gameOuts--;
    }
    public Team getAwayTeam() {
        return awayTeam;
    }
    public Team getHomeTeam() {
        return homeTeam;
    }
    public int getGameOuts() {
        return gameOuts;
    }
    public void setGameOuts(int gameOuts) {
        this.gameOuts = gameOuts;
    }
    public int getInningNumber() {return inningNumber;}
    public void setInningNumber(int inningNumber) {this.inningNumber = inningNumber;}
    public void increaseInningNumber() {
        this.inningNumber++;
    }
    public void decreaseInningNumber() {this.inningNumber--;}
}
