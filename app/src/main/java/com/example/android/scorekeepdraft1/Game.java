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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.scorekeepdraft1.undoredo.AtBat;
import com.example.android.scorekeepdraft1.undoredo.UndoRedoManager;

import com.example.android.scorekeepdraft1.data.PlayerStatsContract.PlayerStatsEntry;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.data;
import static android.os.Build.VERSION_CODES.M;
import static android.transition.Fade.IN;

/**
 * @author Eddie
 */
public class Game extends AppCompatActivity /*implements LoaderManager.LoaderCallbacks<Cursor>*/ {

    private Cursor mCursor;

    private TextView scoreboard;
    private TextView nowBatting;
    private TextView outsDisplay;
    private TextView inningDisplay;
    private TextView avgDisplay;
    private TextView rbiDisplay;
    private TextView runDisplay;
    private TextView hrDisplay;

    //TODO:CHANGE TO RADIO BUTTONS (WITH SUBMIT AND RESET BUTTONS)
    private Button setSingle;
    private Button setDouble;
    private Button setTriple;
    private Button setHR;
    private Button setError;
    private Button setFC;
    private Button setBB;
    private Button setSF;
    private Button setOut;

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
    private String[] listOfPlayers = {"Ed", "Kosta", "Ag", "Yusk", "Josh", "Isaac", "Meech", "Iva", "Rolien", "Adam", "Dom", "Jay"};
    private Team awayTeam = new Team("Isotopes");
    private Team homeTeam = new Team("Purptopes");

    private int inningNumber = 1;
    private int gameOuts = 0;
    private Player[] onBase;
    private Player currentPlayer;
    private Team currentTeam;

    private NumberFormat formatter = new DecimalFormat("#.000");


    private UndoRedoManager manager;
    private boolean unDoing = false;
    private AtBat currentAB;

    //TODO: ADD DRAG AND DROP DISPLAY/LOGIC FOR BASES


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        onBase = new Player[5];

        scoreboard = (TextView) findViewById(R.id.scoreboard);
        nowBatting = (TextView) findViewById(R.id.nowbatting);
        outsDisplay = (TextView) findViewById(R.id.num_of_outs);
        inningDisplay = (TextView) findViewById(R.id.inning);
        avgDisplay = (TextView) findViewById(R.id.avgdisplay);
        rbiDisplay = (TextView) findViewById(R.id.rbidisplay);
        runDisplay = (TextView) findViewById(R.id.rundisplay);
        hrDisplay = (TextView) findViewById(R.id.hrdisplay);

        setSingle = (Button) findViewById(R.id.single);
        setDouble = (Button) findViewById(R.id.dbl);
        setTriple = (Button) findViewById(R.id.triple);
        setHR = (Button) findViewById(R.id.hr);
        setError = (Button) findViewById(R.id.error);
        setFC = (Button) findViewById(R.id.fc);
        setBB = (Button) findViewById(R.id.bb);
        setSF = (Button) findViewById(R.id.sf);
        setOut = (Button) findViewById(R.id.out);


        setSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("1b");
                nextBatter();
            }
        });
        setDouble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("2b");
                nextBatter();
            }
        });
        setTriple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("3b");
                nextBatter();
            }
        });
        setHR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("hr");
                addRBI();
                addRun(currentPlayer.getName());
                nextBatter();
            }
        });
        setError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("out");
                nextBatter();
            }
        });
        setFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("out");
                nextBatter();
            }
        });
        setBB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("bb");
                nextBatter();
            }
        });
        setSF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("sf");
                nextBatter();
            }
        });
        setOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePlayerStats("out");
                addOut();
            }
        });
        findViewById(R.id.second_display).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.home_display).setVisibility(View.VISIBLE);
            }
        });

        // Assign the touch listener to your view which you want to move

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


    private boolean firstOccupied = false;
    private boolean secondOccupied = false;
    private boolean thirdOccupied = false;

    public void basesAllSet() {
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
                    if (v.getId() == R.id.trash) {
                        if (eventView instanceof TextView) {
                            TextView draggedView = (TextView) eventView;
                            draggedView.setText("");
                        } else {
                            batterDisplay.setVisibility(View.INVISIBLE);
                        }
                        addOut();
                    } else {
                        if (eventView instanceof TextView) {
                            TextView draggedView = (TextView) eventView;
                            movedPlayer = draggedView.getText().toString();
                            dropPoint.setText(movedPlayer);
                            draggedView.setText("");
                        } else {
                            dropPoint.setText(currentPlayer.getName());
                            batterDisplay.setVisibility(View.INVISIBLE);
                        }
                    }
                    v.setBackgroundColor(Color.TRANSPARENT);
                    if (dropPoint == homeDisplay) {
                        Toast.makeText(Game.this, "HOMIE!!!", Toast.LENGTH_SHORT).show();
                        if (eventView instanceof TextView) {
                            addRun(movedPlayer);
                        } else {
                            addRun(currentPlayer.getName());
                        }
                        addRBI();
                        homeDisplay.setText("");
                    }
                    basesAllSet();
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
            basesAllSet();

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
                    Toast.makeText(Game.this, "SOMETHING WENT WRONG WITH THE SWITCH", Toast.LENGTH_LONG).show();
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


    public void addRBI() {
        updatePlayerStats("rbi");
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

        currentTeam.addRun();
        scoreboard.setText(awayTeam.getName() + " " + awayTeam.getCurrentRuns() + "    " + homeTeam.getName() + " " + homeTeam.getCurrentRuns());
        startCursor();
    }


    public void startGame() {
        for (int i = 0; i < listOfPlayers.length; i++) {
            if (i % 2 == 0) {
                awayTeam.addPlayer(new Player(listOfPlayers[i]));
            } else {
                homeTeam.addPlayer(new Player(listOfPlayers[i]));
            }
        }
        awayTeam.setCurrentRuns(0);
        homeTeam.setCurrentRuns(0);

        //this.onBase = new Player[5];
        //this.manager = new UndoRedoManager(this);

        currentTeam = awayTeam;
        currentPlayer = awayTeam.getLineup().get(0);

        scoreboard.setText(awayTeam.getName() + " " + awayTeam.getCurrentRuns() + "    " + homeTeam.getName() + " " + homeTeam.getCurrentRuns());

        try {
            startCursor();

            setDisplays();
        } catch (Exception e) {
            Toast.makeText(Game.this, "Adding a temporary database first!", Toast.LENGTH_LONG).show();
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
        currentTeam.increaseIndex();
        if (currentTeam.getIndex() >= currentTeam.getLineup().size()) {
            currentTeam.setIndex(0);
        }

        currentPlayer = currentTeam.getLineup().get(currentTeam.getIndex());

        startCursor();
        setDisplays();
    }

    public void startCursor() {
        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {currentPlayer.getName()};

        mCursor = getContentResolver().query(
                PlayerStatsEntry.CONTENT_URI1, null,
                selection, selectionArgs, null
        );
        mCursor.moveToNext();
    }

    public void updatePlayerStats(String action) {
        ContentValues values = new ContentValues();
        mCursor.moveToFirst();
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
            case "rbi":
                valueIndex = mCursor.getColumnIndex(PlayerStatsEntry.COLUMN_RBI);
                newValue = mCursor.getInt(valueIndex) + 1;
                values.put(PlayerStatsEntry.COLUMN_RBI, newValue);
                break;
            default:
                Toast.makeText(Game.this, "SOMETHING FUCKED UP BIG TIME!!!", Toast.LENGTH_LONG).show();
                break;
        }

        String selection = PlayerStatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs = {currentPlayer.getName()};

        getContentResolver().update(
                PlayerStatsEntry.CONTENT_URI1,
                values,
                selection,
                selectionArgs
        );
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

        nowBatting.setText("Now batting: " + currentPlayer + " (" + name);
        avgDisplay.setText("AVG: " + formatter.format(avg));
        hrDisplay.setText("HR: " + displayHR);
        rbiDisplay.setText("RBI: " + displayRBI);
        runDisplay.setText("R: " + displayRun);
    }

    public void nextInning() {
        gameOuts = 0;
        for (int i = 0; i < onBase.length; i++) {
            onBase[i] = null;
        }
        if (inningNumber >= 6) {
            Toast.makeText(Game.this, "GAME OVER", Toast.LENGTH_LONG).show();
        }
        if (currentTeam == awayTeam) {
            currentTeam = homeTeam;
        } else {
            currentTeam = awayTeam;
            inningNumber++;
            inningDisplay.setText(String.valueOf(inningNumber));
        }
        nextBatter();
    }

    public double calculateAverage(int singles, int doubles, int triples, int hrs, int outs, int errors) {
        double hits = (double) (singles + doubles + triples + hrs);
        return (hits / (outs + errors + hits));
    }

    public void addOut() {
        gameOuts++;
        Toast.makeText(Game.this, "Outs = " + gameOuts, Toast.LENGTH_SHORT).show();
        //AtBat.addPlay(new Play(currentPlayer, currentTeam, "o"));
        if (gameOuts >= 3) {
            nextInning();
        } else {
            nextBatter();
        }
        outsDisplay.setText(gameOuts + "outs");
    }

    public void subtractOut() {
        gameOuts--;
    }
    public void editOnBase(Player[] basesCopied) {System.arraycopy(basesCopied, 1, this.onBase, 1, 4);}
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
    public void setInningNumber(int inningNumber) {this.inningNumber = inningNumber;}
    public int getInningNumber() {return inningNumber;}
    public void increaseInningNumber() {
        this.inningNumber++;
    }
    public void decreaseInningNumber() {
        this.inningNumber--;
    }

}
