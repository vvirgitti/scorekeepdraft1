package com.example.android.scorekeepdraft1.fragments;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.scorekeepdraft1.R;
import com.example.android.scorekeepdraft1.activities.LeagueGameActivity;
import com.example.android.scorekeepdraft1.activities.TeamGameActivity;
import com.example.android.scorekeepdraft1.activities.TeamManagerActivity;
import com.example.android.scorekeepdraft1.activities.UserSettingsActivity;
import com.example.android.scorekeepdraft1.adapters_listeners_etc.SetLineupAdapter;
import com.example.android.scorekeepdraft1.data.StatsContract;
import com.example.android.scorekeepdraft1.data.StatsContract.StatsEntry;
import com.example.android.scorekeepdraft1.dialogs.AddNewPlayersDialogFragment;
import com.example.android.scorekeepdraft1.dialogs.GameSettingsDialogFragment;
import com.example.android.scorekeepdraft1.objects.MainPageSelection;
import com.example.android.scorekeepdraft1.objects.Player;

import java.util.ArrayList;
import java.util.List;

public class LineupFragment extends Fragment {

    private SetLineupAdapter leftListAdapter;
    private SetLineupAdapter rightListAdapter;

    private RecyclerView rvLeftLineup;
    private RecyclerView rvRightLineup;
    private boolean sortLineup;

    private TextView gameSummaryView;
    private TextView inningsView;
    private TextView orderView;

    private List<Player> mLineup;
    private List<Player> mBench;
    private String mTeamName;
    private String mTeamID;
    private int mType;
    private String mSelectionID;
    private boolean inGame;

    private static final String KEY_TEAM_NAME = "team_name";
    private static final String KEY_TEAM_ID = "team_id";
    private static final String KEY_INGAME = "ingame";

    public LineupFragment() {
        // Required empty public constructor
    }

    public static LineupFragment newInstance(String selectionID, int selectionType,
                                             String teamName, String teamID, boolean isInGame) {
        Bundle args = new Bundle();
        args.putString(MainPageSelection.KEY_SELECTION_ID, selectionID);
        args.putInt(MainPageSelection.KEY_SELECTION_TYPE, selectionType);
        args.putString(KEY_TEAM_NAME, teamName);
        args.putString(KEY_TEAM_ID, teamID);
        args.putBoolean(KEY_INGAME, isInGame);
        LineupFragment fragment = new LineupFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        if (args != null) {
            mSelectionID = args.getString(MainPageSelection.KEY_SELECTION_ID);
            mType = args.getInt(MainPageSelection.KEY_SELECTION_TYPE);
            mTeamName = args.getString(KEY_TEAM_NAME);
            mTeamID = args.getString(KEY_TEAM_ID);
            inGame = args.getBoolean(KEY_INGAME);
        } else {
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_lineup, container, false);

        rvLeftLineup = rootView.findViewById(R.id.rvLeft);
        rvRightLineup = rootView.findViewById(R.id.rvRight);

        Button lineupSubmitButton = rootView.findViewById(R.id.lineup_submit);
        lineupSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inGame) {
                    onSubmitEdit();
                } else {
                    onSubmitLineup();
                }
            }
        });

        if(inGame) {
            lineupSubmitButton.setText("Save Edit");
        }

        final TextView teamNameTextView = rootView.findViewById(R.id.team_name_display);
        teamNameTextView.setText(mTeamName);

        final FloatingActionButton addPlayersButton = rootView.findViewById(R.id.btn_start_adder);
        addPlayersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTeamFragment(mTeamName, mTeamID);
            }
        });

        return rootView;
    }

    public void onSubmitEdit() {
        if (isLineupOK()) {
            setNewLineupToTempDB(getPreviousLineup(mTeamID));
            Intent intent;
            SharedPreferences gamePreferences = getActivity().getSharedPreferences(mSelectionID + StatsEntry.GAME, Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = gamePreferences.edit();
            if (mType == MainPageSelection.TYPE_LEAGUE) {
                String awayTeam = gamePreferences.getString("keyAwayTeam", null);
                String homeTeam = gamePreferences.getString("keyHomeTeam", null);
                int sortArgument = gamePreferences.getInt("keyGenderSort", 0);

                switch (sortArgument) {
                    case 3:
                        if (mTeamID.equals(awayTeam)) {
                            sortArgument = 2;
                        } else if (mTeamID.equals(homeTeam)) {
                            sortArgument = 1;
                        }
                        break;

                    case 2:
                        if (mTeamID.equals(homeTeam)) {
                            sortArgument = 0;
                        }
                        break;

                    case 1:
                        if (mTeamID.equals(awayTeam)) {
                            sortArgument = 0;
                        }
                        break;
                }

                intent = new Intent(getActivity(), LeagueGameActivity.class);
                editor.putInt("keyGenderSort", sortArgument);
            } else {
                intent = new Intent(getActivity(), TeamGameActivity.class);
                editor.putBoolean("keyGenderSort", false);
            }
            editor.commit();
            intent.putExtra("edited", true);
            startActivity(intent);
            getActivity().finish();
        }
    }

    public void onSubmitLineup() {
        if (mType == MainPageSelection.TYPE_TEAM) {
            int genderSorter = getGenderSorter();

            if (isLineupOK()) {
                clearGameDB();
                boolean lineupCheck = addTeamToTempDB(genderSorter);
                if (lineupCheck) {
                    startGame(isHome());
                }
            } else {
                Toast.makeText(getActivity(), "Add more players to lineup first.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            updateAndSubmitLineup();
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLineup == null) {
            mLineup = new ArrayList<>();
        } else {
            mLineup.clear();
        }

        if(mBench == null) {
            mBench = new ArrayList<>();
        } else {
            mBench.clear();
        }

        String selection = StatsEntry.COLUMN_TEAM_FIRESTORE_ID + "=?";
        String[] selectionArgs = new String[]{mTeamID};
        String sortOrder = StatsEntry.COLUMN_ORDER + " ASC";

        Cursor cursor = getActivity().getContentResolver().query(StatsEntry.CONTENT_URI_PLAYERS,
                null, selection, selectionArgs, sortOrder);

        while (cursor.moveToNext()) {
            Player player = new Player(cursor, false);
            int playerOrder = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_ORDER);
            if (playerOrder > 50) {
                mBench.add(player);
            } else {
                mLineup.add(player);
            }
        }
        cursor.close();
        updateLineupRV();
        updateBenchRV();

        if (mType == MainPageSelection.TYPE_TEAM && !inGame) {
            SharedPreferences settingsPreferences = getActivity()
                    .getSharedPreferences(mSelectionID + StatsEntry.SETTINGS, Context.MODE_PRIVATE);
            final int innings = settingsPreferences.getInt(StatsEntry.INNINGS, 7);
            final int genderSorter = settingsPreferences.getInt(StatsEntry.COLUMN_GENDER, 0);
            inningsView = getView().findViewById(R.id.innings_view);
            inningsView.setVisibility(View.VISIBLE);
            orderView = getView().findViewById(R.id.gender_lineup_view);
            gameSummaryView = getView().findViewById(R.id.current_game_view);
            setGameSettings(innings, genderSorter);

            Button lineupSubmitButton = getView().findViewById(R.id.lineup_submit);
            lineupSubmitButton.setText(R.string.start);
            View radioButtonGroup = getView().findViewById(R.id.radiobtns_away_or_home_team);
            radioButtonGroup.setVisibility(View.VISIBLE);

            Button continueGameButton = getView().findViewById(R.id.continue_game);
            continueGameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), TeamGameActivity.class);
                    startActivity(intent);
                }
            });
            continueGameButton.setVisibility(View.VISIBLE);

            cursor = getActivity().getContentResolver().query(StatsEntry.CONTENT_URI_GAMELOG,
                    null, null, null, null);
            if (cursor.moveToLast()) {
                int awayRuns = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_AWAY_RUNS);
                int homeRuns = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_HOME_RUNS);
                setGameSummaryView(awayRuns, homeRuns);
                continueGameButton.setVisibility(View.VISIBLE);
                gameSummaryView.setVisibility(View.VISIBLE);
            } else {
                continueGameButton.setVisibility(View.GONE);
                gameSummaryView.setVisibility(View.GONE);
            }
            cursor.close();
        }
    }

    public void updateBench(List<Player> players) {
        mBench.addAll(players);
        updateBenchRV();
    }

    private void createTeamFragment(String teamName, String teamID) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DialogFragment newFragment = AddNewPlayersDialogFragment.newInstance(teamName, teamID);
        newFragment.show(fragmentTransaction, "");
    }

    public void setGameSettings(int innings, int gendersorter) {
        if(inningsView == null) {
            return;
        }
        String inningsText = "Innings: " +  innings;
        inningsView.setText(inningsText);
        setGenderSettingDisplay(gendersorter);
    }

    private void setGenderSettingDisplay(int i) {
        if(orderView == null) {
            return;
        }
        if (i == 0) {
            orderView.setVisibility(View.INVISIBLE);
            return;
        }
        orderView.setVisibility(View.VISIBLE);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Order: ");
        for (int index = 0; index < i; index++) {
            stringBuilder.append("<font color='#6fa2ef'>B</font>");
        }
        stringBuilder.append("<font color='#f99da2'>G</font>");
        String order = stringBuilder.toString();
        orderView.setText(Html.fromHtml(order));
    }

    private void setGameSummaryView(int awayRuns, int homeRuns){
        SharedPreferences savedGamePreferences = getActivity()
                .getSharedPreferences(mSelectionID + StatsEntry.GAME, Context.MODE_PRIVATE);
        int inningNumber = savedGamePreferences.getInt("keyInningNumber", 2);
        inningNumber = inningNumber/2;
        boolean isHome = savedGamePreferences.getBoolean("isHome", false);
        String awayTeamName = "A";
        String homeTeamName = "H";
        if(isHome) {
            homeTeamName = mTeamName;
        } else {
            awayTeamName = mTeamName;
        }
        String summary = awayTeamName + ": " + awayRuns + "    "  + homeTeamName + ": " + homeRuns + "\nInning: " + inningNumber;
        gameSummaryView.setText(summary);
    }


    private void startGame(boolean isHome) {
        Intent intent = new Intent(getActivity(), TeamGameActivity.class);
        intent.putExtra("isHome", isHome);
        intent.putExtra("sortArgument", sortLineup);
        startActivity(intent);
    }


    private boolean isLineupOK() {
        return updateAndSubmitLineup() > 3;
    }

    private int getGenderSorter() {
        SharedPreferences genderPreferences = getActivity()
                .getSharedPreferences(mSelectionID + StatsEntry.SETTINGS, Context.MODE_PRIVATE);
        return genderPreferences.getInt(StatsEntry.COLUMN_GENDER, 0);
    }

    private boolean isHome() {
        RadioGroup radioGroup = getView().findViewById(R.id.radiobtns_away_or_home_team);
        int id = radioGroup.getCheckedRadioButtonId();
        switch (id) {
            case R.id.radio_away:
                return false;
            case R.id.radio_home:
                return true;
            default:
                Log.e("lineup", "Radiobutton error");
        }
        return false;
    }

    private void clearGameDB() {
        getActivity().getContentResolver().delete(StatsEntry.CONTENT_URI_TEMP, null, null);
        getActivity().getContentResolver().delete(StatsEntry.CONTENT_URI_GAMELOG, null, null);
        SharedPreferences savedGamePreferences = getActivity().getSharedPreferences(mSelectionID + StatsEntry.GAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = savedGamePreferences.edit();
        editor.clear();
        editor.commit();
    }

    private List<Player> getPreviousLineup(String teamID) {

        String selection = StatsEntry.COLUMN_TEAM_FIRESTORE_ID + "=?";
        String[] selectionArgs = new String[]{teamID};
        Cursor cursor = getActivity().getContentResolver().query(StatsEntry.CONTENT_URI_TEMP,
                null, selection, selectionArgs, null);

        List<Player> previousLineup = new ArrayList<>();

        while (cursor.moveToNext()) {
            previousLineup.add(new Player(cursor, true));
        }
        return previousLineup;
    }

    private boolean setNewLineupToTempDB(List<Player> previousLineup) {

        List<Player> lineup = getLineup();

        String selection = StatsEntry.COLUMN_TEAM_FIRESTORE_ID + "=?";
        String[] selectionArgs = new String[]{mTeamID};

        ContentResolver contentResolver = getActivity().getContentResolver();
        contentResolver.delete(StatsEntry.CONTENT_URI_TEMP, selection, selectionArgs);

        for (int i = 0; i < lineup.size(); i++) {
            Player player = lineup.get(i);
            long playerId = player.getPlayerId();
            String playerName = player.getName();
            Log.d("xxx", "getLU: " + playerName);
            int gender = player.getGender();
            String firestoreID = player.getFirestoreID();

            ContentValues values = new ContentValues();
            values.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);
            values.put(StatsEntry.COLUMN_PLAYERID, playerId);
            values.put(StatsEntry.COLUMN_NAME, playerName);
            values.put(StatsEntry.COLUMN_TEAM, mTeamName);
            values.put(StatsEntry.COLUMN_TEAM_FIRESTORE_ID, mTeamID);
            values.put(StatsEntry.COLUMN_ORDER, i + 1);
            values.put(StatsEntry.COLUMN_GENDER, gender);

            Player existingPlayer = checkIfPlayerExists(playerId, previousLineup);
            if (existingPlayer != null) {
                values.put(StatsEntry.COLUMN_HR, existingPlayer.getHrs());
                values.put(StatsEntry.COLUMN_3B, existingPlayer.getTriples());
                values.put(StatsEntry.COLUMN_2B, existingPlayer.getDoubles());
                values.put(StatsEntry.COLUMN_1B, existingPlayer.getSingles());
                values.put(StatsEntry.COLUMN_BB, existingPlayer.getWalks());
                values.put(StatsEntry.COLUMN_OUT, existingPlayer.getOuts());
                values.put(StatsEntry.COLUMN_SF, existingPlayer.getSacFlies());
                values.put(StatsEntry.COLUMN_RUN, existingPlayer.getRuns());
                values.put(StatsEntry.COLUMN_RBI, existingPlayer.getRbis());
                previousLineup.remove(existingPlayer);
            }

            Log.d("xxx", "currentLineup: " + playerName);
            contentResolver.insert(StatsEntry.CONTENT_URI_TEMP, values);
        }

        if (!previousLineup.isEmpty()) {
            for (int i = 0; i < previousLineup.size(); i++) {
                Player existingPlayer = previousLineup.get(i);
                ContentValues values = new ContentValues();

                values.put(StatsEntry.COLUMN_FIRESTORE_ID, existingPlayer.getFirestoreID());
                values.put(StatsEntry.COLUMN_PLAYERID, existingPlayer.getPlayerId());
                values.put(StatsEntry.COLUMN_NAME, existingPlayer.getName());
                values.put(StatsEntry.COLUMN_TEAM, mTeamName);
                values.put(StatsEntry.COLUMN_TEAM_FIRESTORE_ID, existingPlayer.getTeamfirestoreid());
                values.put(StatsEntry.COLUMN_ORDER, 999);
                values.put(StatsEntry.COLUMN_GENDER, existingPlayer.getGender());
                values.put(StatsEntry.COLUMN_HR, existingPlayer.getHrs());
                values.put(StatsEntry.COLUMN_3B, existingPlayer.getTriples());
                values.put(StatsEntry.COLUMN_2B, existingPlayer.getDoubles());
                values.put(StatsEntry.COLUMN_1B, existingPlayer.getSingles());
                values.put(StatsEntry.COLUMN_BB, existingPlayer.getWalks());
                values.put(StatsEntry.COLUMN_OUT, existingPlayer.getOuts());
                values.put(StatsEntry.COLUMN_SF, existingPlayer.getSacFlies());
                values.put(StatsEntry.COLUMN_RUN, existingPlayer.getRuns());
                values.put(StatsEntry.COLUMN_RBI, existingPlayer.getRbis());

                contentResolver.insert(StatsEntry.CONTENT_URI_TEMP, values);
                Log.d("xxx", "removedPlayer: " + existingPlayer.getName());
            }
            previousLineup.clear();
        }
        return true;
    }

    private Player checkIfPlayerExists(long playerID, List<Player> players) {
        for (Player player : players) {
            if (playerID == player.getPlayerId()) {
                return player;
            }
        }
        return null;
    }


    private boolean addTeamToTempDB(int requiredFemale) {
        List<Player> lineup = getLineup();
        ContentResolver contentResolver = getActivity().getContentResolver();
        int females = 0;
        int males = 0;
        int malesInRow = 0;
        int firstMalesInRow = 0;
        boolean beforeFirstFemale = true;
        boolean notProperOrder = false;
        sortLineup = false;

        for (int i = 0; i < lineup.size(); i++) {
            Player player = lineup.get(i);
            long playerId = player.getPlayerId();
            String playerName = player.getName();
            int gender = player.getGender();
            String firestoreID = player.getFirestoreID();

            ContentValues values = new ContentValues();
            values.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);
            values.put(StatsEntry.COLUMN_PLAYERID, playerId);
            values.put(StatsEntry.COLUMN_NAME, playerName);
            values.put(StatsEntry.COLUMN_TEAM, mTeamName);
            values.put(StatsEntry.COLUMN_TEAM_FIRESTORE_ID, mTeamID);
            values.put(StatsEntry.COLUMN_ORDER, i + 1);
            values.put(StatsEntry.COLUMN_GENDER, gender);
            contentResolver.insert(StatsEntry.CONTENT_URI_TEMP, values);

            if (gender == 0) {
                males++;
                malesInRow++;
                if (beforeFirstFemale) {
                    firstMalesInRow++;
                }
                if (malesInRow > requiredFemale) {
                    notProperOrder = true;
                }
            } else {
                females++;
                malesInRow = 0;
                beforeFirstFemale = false;
            }
        }

        if (requiredFemale < 1) {
            return true;
        }

        int lastMalesInRow = malesInRow;
        if (firstMalesInRow + lastMalesInRow > requiredFemale) {
            notProperOrder = true;
        }
        if (notProperOrder) {
            if (females * requiredFemale >= males) {
                Toast.makeText(getActivity(),
                        "Please set " + mTeamName + "'s lineup properly or change gender rules",
                        Toast.LENGTH_LONG).show();
                return false;
            }
            sortLineup = true;
        }
        return true;
    }

    private ArrayList<Player> getLineup() {
        ArrayList<Player> lineup = new ArrayList<>();
        try {
            String selection = StatsEntry.COLUMN_TEAM_FIRESTORE_ID + "=?";
            String[] selectionArgs = new String[]{mTeamID};
            String sortOrder = StatsEntry.COLUMN_ORDER + " ASC";

            Cursor cursor = getActivity().getContentResolver().query(StatsEntry.CONTENT_URI_PLAYERS, null,
                    selection, selectionArgs, sortOrder);
            while (cursor.moveToNext()) {
                int order = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_ORDER);
                if (order < 50) {
                    lineup.add(new Player(cursor, false));
                }
            }
            cursor.close();
            return lineup;
        } catch (Exception e) {
            Toast.makeText(getActivity(), "woops  " + e, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public void updateLineupRV() {
        if (leftListAdapter == null) {
            int genderSorter = getGenderSorter();

            rvLeftLineup.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));
            leftListAdapter = new SetLineupAdapter(mLineup, getContext(), false, genderSorter);
            rvLeftLineup.setAdapter(leftListAdapter);
            rvLeftLineup.setOnDragListener(leftListAdapter.getDragInstance());
        } else {
            leftListAdapter.notifyDataSetChanged();
        }
    }

    public void updateBenchRV() {
        if (rightListAdapter == null) {
            int genderSorter = getGenderSorter();

            rvRightLineup.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));

            rightListAdapter = new SetLineupAdapter(mBench, getContext(), true, genderSorter);
            rvRightLineup.setAdapter(rightListAdapter);
            rvRightLineup.setOnDragListener(rightListAdapter.getDragInstance());
        } else {
            rightListAdapter.notifyDataSetChanged();
        }
    }

    public void changeColorsRV(boolean genderSettingsOn) {
        boolean update = true;
        if (leftListAdapter != null && rightListAdapter != null) {
            update = leftListAdapter.changeColors(genderSettingsOn);
            rightListAdapter.changeColors(genderSettingsOn);
        }
        if (update) {
            updateLineupRV();
            updateBenchRV();
        }
    }

    private int updateAndSubmitLineup() {
        String selection = StatsEntry.COLUMN_NAME + "=?";
        String[] selectionArgs;

        int i = 1;
        List<Player> lineupList = getLeftListAdapter().getPlayerList();
        for (Player player : lineupList) {
            String name = player.getName();
            selectionArgs = new String[]{name};
            ContentValues values = new ContentValues();
            values.put(StatsEntry.COLUMN_ORDER, i);
            getActivity().getContentResolver().update(StatsEntry.CONTENT_URI_PLAYERS, values,
                    selection, selectionArgs);
            i++;
        }

        i = 99;
        List<Player> benchList = getRightListAdapter().getPlayerList();
        for (Player player : benchList) {
            String name = player.getName();
            selectionArgs = new String[]{name};
            ContentValues values = new ContentValues();
            values.put(StatsContract.StatsEntry.COLUMN_ORDER, i);
            getActivity().getContentResolver().update(StatsContract.StatsEntry.CONTENT_URI_PLAYERS, values,
                    selection, selectionArgs);
        }

        return lineupList.size();
    }

    public SetLineupAdapter getLeftListAdapter() {
        return leftListAdapter;
    }

    public SetLineupAdapter getRightListAdapter() {
        return rightListAdapter;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_league, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Activity activity = getActivity();
        if (!(activity instanceof TeamManagerActivity)) {
            menu.findItem(R.id.action_export_stats).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_user_settings:
                Intent settingsIntent = new Intent(getActivity(), UserSettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.change_game_settings:
                SharedPreferences settingsPreferences = getActivity()
                        .getSharedPreferences(mSelectionID + StatsEntry.SETTINGS, Context.MODE_PRIVATE);
                int innings = settingsPreferences.getInt(StatsEntry.INNINGS, 7);
                int genderSorter = settingsPreferences.getInt(StatsEntry.COLUMN_GENDER, 0);
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                DialogFragment newFragment = GameSettingsDialogFragment.newInstance(innings, genderSorter, mSelectionID);
                newFragment.show(fragmentTransaction, "");
                return true;
            case R.id.action_export_stats:
                Activity activity = getActivity();
                if (activity instanceof TeamManagerActivity) {
                    TeamManagerActivity teamManagerActivity = (TeamManagerActivity) activity;
                    teamManagerActivity.startExport(mTeamName);
                    return true;
                }
                return false;
        }
        return false;
    }

    public void removePlayerFromTeam(String playerFirestoreID){

        Player player = new Player(null, -1, playerFirestoreID);

        if(mLineup.contains(player)) {
            mLineup.remove(player);
            updateLineupRV();

        } else if(mBench.contains(player)) {
            mBench.remove(player);
            updateBenchRV();

        } else {
            Log.d("xxx", "error with lineupFragment removePlayerFromTeam");
        }

    }
}