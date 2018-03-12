package com.example.android.softballstatkeeper.fragments;


import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.softballstatkeeper.MyApp;
import com.example.android.softballstatkeeper.R;
import com.example.android.softballstatkeeper.activities.LeagueManagerActivity;
import com.example.android.softballstatkeeper.activities.PlayerManagerActivity;
import com.example.android.softballstatkeeper.activities.PlayerPagerActivity;
import com.example.android.softballstatkeeper.activities.TeamPagerActivity;
import com.example.android.softballstatkeeper.activities.UserSettingsActivity;
import com.example.android.softballstatkeeper.data.FirestoreHelper;
import com.example.android.softballstatkeeper.data.StatsContract;
import com.example.android.softballstatkeeper.data.StatsContract.StatsEntry;
import com.example.android.softballstatkeeper.dialogs.ChangeTeamDialogFragment;
import com.example.android.softballstatkeeper.dialogs.DeleteConfirmationDialogFragment;
import com.example.android.softballstatkeeper.dialogs.EditNameDialogFragment;
import com.example.android.softballstatkeeper.objects.MainPageSelection;
import com.example.android.softballstatkeeper.objects.Player;
import com.example.android.softballstatkeeper.objects.Team;
import com.squareup.leakcanary.RefWatcher;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private NumberFormat formatter = new DecimalFormat("#.000");
    private static final int EXISTING_PLAYER_LOADER = 0;
    private Uri mCurrentPlayerUri;
    private int mLevel;
    private String mSelectionID;
    private String teamString;
    private String playerName;
    private String firestoreID;
    private String teamFirestoreID;
    private int gender;
    private int mSelectionType;
    private TextView resultCountText;
    private int resultCount;
    private String result;
    private TextView resultText;
    private TextView nameView;
    private ImageView playerImage;

    private RadioGroup group1;
    private RadioGroup group2;
    private OnFragmentInteractionListener mListener;

    private static final String KEY_PLAYER_URI = "playerURI";

    public PlayerFragment() {
        // Required empty public constructor
    }

    public static PlayerFragment newInstance(String leagueID, int leagueType, int level, Uri uri) {
        Bundle args = new Bundle();
        args.putInt(MainPageSelection.KEY_SELECTION_TYPE, leagueType);
        args.putInt(MainPageSelection.KEY_SELECTION_LEVEL, level);
        args.putString(MainPageSelection.KEY_SELECTION_ID, leagueID);
        args.putString(KEY_PLAYER_URI, uri.toString());
        PlayerFragment fragment = new PlayerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static PlayerFragment newInstance(int leagueType, String playerName) {
        Bundle args = new Bundle();
        args.putInt(MainPageSelection.KEY_SELECTION_TYPE, leagueType);
        args.putInt(MainPageSelection.KEY_SELECTION_LEVEL, 5);
        args.putString(MainPageSelection.KEY_SELECTION_NAME, playerName);
        PlayerFragment fragment = new PlayerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        mSelectionType = args.getInt(MainPageSelection.KEY_SELECTION_TYPE);
        mLevel = args.getInt(MainPageSelection.KEY_SELECTION_LEVEL);
        if (mSelectionType == MainPageSelection.TYPE_PLAYER) {
            playerName = args.getString(MainPageSelection.KEY_SELECTION_NAME);
            mCurrentPlayerUri = StatsEntry.CONTENT_URI_PLAYERS;
        } else {
            String uriString = args.getString(KEY_PLAYER_URI);
            mCurrentPlayerUri = Uri.parse(uriString);
            mSelectionID = args.getString(MainPageSelection.KEY_SELECTION_ID);
        }
        Log.d("zzz", "PlayerFragment onCreate " + mCurrentPlayerUri.toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("zzz", "PlayerFragment onCreateView " + mCurrentPlayerUri.toString());
        getLoaderManager().initLoader(EXISTING_PLAYER_LOADER, null, this);
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("zzz", "PlayerFragment onActivityCreated " + mCurrentPlayerUri.toString());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d("zzz", "PlayerFragment onCreateLoader " + mCurrentPlayerUri.toString());
        return new CursorLoader(
                getActivity(),
                mCurrentPlayerUri,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d("zzz", "PlayerFragment onLoadFinishedStart " + mCurrentPlayerUri.toString());
        View rootView = getView();
        nameView = rootView.findViewById(R.id.player_name);

        if (cursor.moveToFirst()) {
            Player player = new Player(cursor, false);

            playerName = player.getName();
            teamString = player.getTeam();
            gender = player.getGender();
            firestoreID = player.getFirestoreID();
            teamFirestoreID = player.getTeamfirestoreid();

            TextView abView = rootView.findViewById(R.id.playerboard_ab);
            TextView hitView = rootView.findViewById(R.id.playerboard_hit);
            TextView hrView = rootView.findViewById(R.id.player_hr);
            TextView rbiView = rootView.findViewById(R.id.player_rbi);
            TextView runView = rootView.findViewById(R.id.player_runs);
            TextView avgView = rootView.findViewById(R.id.player_avg);
            TextView obpView = rootView.findViewById(R.id.playerboard_obp);
            TextView slgView = rootView.findViewById(R.id.player_slg);
            TextView opsView = rootView.findViewById(R.id.player_ops);
            TextView sglView = rootView.findViewById(R.id.playerboard_1b);
            TextView dblView = rootView.findViewById(R.id.playerboard_2b);
            TextView tplView = rootView.findViewById(R.id.playerboard_3b);
            TextView bbView = rootView.findViewById(R.id.playerboard_bb);
            TextView teamView = rootView.findViewById(R.id.player_team);
            playerImage = rootView.findViewById(R.id.player_image);

            if (mSelectionType == MainPageSelection.TYPE_LEAGUE) {
                teamView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (teamFirestoreID != null) {
                            Intent intent;
                            String selection = StatsEntry.COLUMN_FIRESTORE_ID + "=?";
                            String[] selectionArgs = new String[]{teamFirestoreID};
                            if (teamFirestoreID.equals(StatsEntry.FREE_AGENT)) {
                                intent = new Intent(getActivity(), TeamPagerActivity.class);
                            } else {
                                Cursor cursor = getActivity().getContentResolver().query(StatsEntry.CONTENT_URI_TEAMS,
                                        null, selection, selectionArgs, null);
                                if (cursor.moveToFirst()) {
                                    int teamId = cursor.getInt(cursor.getColumnIndex(StatsEntry._ID));
                                    Uri teamUri = ContentUris.withAppendedId(StatsEntry.CONTENT_URI_TEAMS, teamId);
                                    intent = new Intent(getActivity(), TeamPagerActivity.class);
                                    intent.setData(teamUri);
                                } else {
                                    intent = new Intent(getActivity(), LeagueManagerActivity.class);
                                }
                                cursor.close();
                            }
                            startActivity(intent);
                        } else {
                            Log.d("PlayerActivity", "Error going to team page");
                        }
                    }
                });
            }

            playerName = player.getName();
            nameView.setText(playerName);
            teamView.setText(teamString);
            abView.setText(String.valueOf(player.getABs()));
            hitView.setText(String.valueOf(player.getHits()));
            hrView.setText(String.valueOf(player.getHrs()));
            rbiView.setText(String.valueOf(player.getRbis()));
            runView.setText(String.valueOf(player.getRuns()));
            sglView.setText(String.valueOf(player.getSingles()));
            dblView.setText(String.valueOf(player.getDoubles()));
            tplView.setText(String.valueOf(player.getTriples()));
            bbView.setText(String.valueOf(player.getWalks()));
            avgView.setText(String.valueOf(formatter.format(player.getAVG())));
            obpView.setText(String.valueOf(formatter.format(player.getOBP())));
            slgView.setText(String.valueOf(formatter.format(player.getSLG())));
            opsView.setText(String.valueOf(formatter.format(player.getOPS())));

            if (mSelectionType == MainPageSelection.TYPE_PLAYER) {
                nameView.setTextColor(getResources().getColor(R.color.colorPrimary));
                playerImage.setColorFilter(getResources().getColor(R.color.colorPrimary));
                teamView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.setTeamEdit();
                            editNameDialog(teamString);
                        }
                    }
                });
                setPlayerManager();
            } else {
                setColor();
            }
        } else if (mSelectionType == MainPageSelection.TYPE_PLAYER) {
            ContentValues values = new ContentValues();
            values.put(StatsEntry.COLUMN_NAME, playerName);
            getActivity().getContentResolver().insert(StatsEntry.CONTENT_URI_PLAYERS, values);
            setPlayerManager();
        }
        Log.d("zzz", "PlayerFragment onLoadFinishedEnd " + mCurrentPlayerUri.toString());
    }

    private void setColor() {
        int color;
        if (gender == 0) {
            color = R.color.male;
        } else {
            color = R.color.female;
        }
        nameView.setTextColor(getResources().getColor(color));
        playerImage.setColorFilter(getResources().getColor(color));
    }

    private void setPlayerManager() {
        View playerManager = getView().findViewById(R.id.player_mgr);
        playerManager.setVisibility(View.VISIBLE);
        setRadioButtons(playerManager);

        resultCount = 0;
        resultCountText = playerManager.findViewById(R.id.textview_result_count);
        resultText = playerManager.findViewById(R.id.textview_result_chosen);
        Button submitBtn = playerManager.findViewById(R.id.submit);
        ImageView addBtn = playerManager.findViewById(R.id.btn_add_result);
        ImageView subtractBtn = playerManager.findViewById(R.id.btn_subtract_result);

        subtractBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultCount--;
                resultCountText.setText(String.valueOf(resultCount));
            }
        });
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultCount++;
                resultCountText.setText(String.valueOf(resultCount));
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (result == null) {
                    Toast.makeText(getActivity(), "Please select a result first.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String statEntry;
                Cursor cursor = getActivity().getContentResolver().query(mCurrentPlayerUri,
                        null, null, null, null);
                if (cursor.moveToFirst()) {
                    switch (result) {
                        case "1B":
                            statEntry = StatsEntry.COLUMN_1B;
                            break;
                        case "2B":
                            statEntry = StatsEntry.COLUMN_2B;
                            break;
                        case "3B":
                            statEntry = StatsEntry.COLUMN_3B;
                            break;
                        case "HR":
                            statEntry = StatsEntry.COLUMN_HR;
                            break;
                        case "BB":
                            statEntry = StatsEntry.COLUMN_BB;
                            break;
                        case "Out":
                            statEntry = StatsEntry.COLUMN_OUT;
                            break;
                        case "SF":
                            statEntry = StatsEntry.COLUMN_SF;
                            break;
                        case "Run":
                            statEntry = StatsEntry.COLUMN_RUN;
                            break;
                        case "RBI":
                            statEntry = StatsEntry.COLUMN_RBI;
                            break;
                        default:
                            return;
                    }
                    int currentResultCount = cursor.getInt(cursor.getColumnIndex(statEntry));
                    resultCount += currentResultCount;
                    ContentValues values = new ContentValues();
                    values.put(statEntry, resultCount);
                    getActivity().getContentResolver().update(mCurrentPlayerUri,
                            values, null, null);
                }
                cursor.close();

                resultCount = 0;
                resultCountText.setText(String.valueOf(0));
            }
        });
    }

    public void setRadioButtons(View view) {
        group1 = view.findViewById(R.id.group1);
        group2 = view.findViewById(R.id.group2);
        RadioButton single = view.findViewById(R.id.single);
        RadioButton dbl = view.findViewById(R.id.dbl);
        RadioButton triple = view.findViewById(R.id.triple);
        RadioButton hr = view.findViewById(R.id.hr);
        RadioButton bb = view.findViewById(R.id.bb);
        RadioButton out = view.findViewById(R.id.out);
        RadioButton sf = view.findViewById(R.id.sf);
        RadioButton run = view.findViewById(R.id.run);
        RadioButton rbi = view.findViewById(R.id.rbi);
        single.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group2.clearCheck();
                    result = "1B";
                    resultText.setText(result);
                }
            }
        });
        dbl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group2.clearCheck();
                    result = "2B";
                    resultText.setText(result);
                }
            }
        });
        triple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group2.clearCheck();
                    result = "3B";
                    resultText.setText(result);
                }
            }
        });
        hr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group2.clearCheck();
                    result = "HR";
                    resultText.setText(result);
                }
            }
        });
        bb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group1.clearCheck();
                    result = "BB";
                    resultText.setText(result);
                }
            }
        });
        out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group1.clearCheck();
                    result = "Out";
                    resultText.setText(result);
                }
            }
        });
        sf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group1.clearCheck();
                    result = "SF";
                    resultText.setText(result);
                }
            }
        });
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group1.clearCheck();
                    result = "Run";
                    resultText.setText(result);
                }
            }
        });
        rbi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((RadioButton) view).isChecked();
                if (checked) {
                    group1.clearCheck();
                    result = "RBI";
                    resultText.setText(result);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (levelAuthorized(UserSettingsActivity.LEVEL_VIEW_WRITE)) {
            inflater.inflate(R.menu.menu_player, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_name:
                editNameDialog(playerName);
                return true;
            case R.id.action_change_team:
                if (mSelectionType == MainPageSelection.TYPE_LEAGUE) {
                    changeTeamDialog();
                } else if (mSelectionType == MainPageSelection.TYPE_PLAYER) {
                    if (mListener != null) {
                        mListener.setTeamEdit();
                        editNameDialog(teamString);
                    }
                }
                return true;
            case R.id.action_change_gender:
                if (gender == 0) {
                    gender = 1;
                } else {
                    gender = 0;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(StatsEntry.COLUMN_GENDER, gender);
                contentValues.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);
                int rowsUpdated = getActivity().getContentResolver().update(mCurrentPlayerUri, contentValues, null, null);
                if(rowsUpdated > 0) {
                    new FirestoreHelper(getActivity(), mSelectionID).updateTimeStamps();
                }
                setColor();
                ((PlayerPagerActivity) getActivity()).returnGenderEdit(gender, firestoreID);
                return true;
            case R.id.action_delete_player:
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_export_stats:
                Activity activity = getActivity();
                if (activity instanceof PlayerManagerActivity) {
                    PlayerManagerActivity playerManagerActivity = (PlayerManagerActivity) activity;
                    playerManagerActivity.startExport(playerName);
                    return true;
                }
                return false;
        }
        return super.

                onOptionsItemSelected(item);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!levelAuthorized(UserSettingsActivity.LEVEL_VIEW_WRITE)) {
            return;
        }
        menu.findItem(R.id.action_change_name).setVisible(true);
        menu.findItem(R.id.action_change_gender).setVisible(true);

        if (levelAuthorized(UserSettingsActivity.LEVEL_ADMIN)) {
            menu.findItem(R.id.action_delete_player).setVisible(true);
        }
        if (mSelectionType != MainPageSelection.TYPE_TEAM) {
            menu.findItem(R.id.action_change_team).setVisible(true);
        }
        if (mSelectionType == MainPageSelection.TYPE_PLAYER) {
            menu.findItem(R.id.action_export_stats).setVisible(true);
            menu.findItem(R.id.action_delete_player).setVisible(false);
            menu.findItem(R.id.action_change_gender).setVisible(false);
            menu.findItem(R.id.action_change_gender).setVisible(false);
        }
    }

    private void showDeleteConfirmationDialog() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DialogFragment newFragment = DeleteConfirmationDialogFragment.newInstance(playerName);
        newFragment.show(fragmentTransaction, "");
    }

    private void editNameDialog(String name) {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DialogFragment newFragment = EditNameDialogFragment.newInstance(name);
        newFragment.show(fragmentTransaction, "");
    }

    private void changeTeamDialog() {
        ArrayList<Team> teams = new ArrayList<>();

        String sortOrder = StatsEntry.COLUMN_NAME + " COLLATE NOCASE ASC";
        Cursor cursor = getActivity().getContentResolver().query(StatsEntry.CONTENT_URI_TEAMS,
                null, null, null, sortOrder);

        while (cursor.moveToNext()) {
            teams.add(new Team(cursor));
        }
//        teams.add(new Team(getString(R.string.waivers), StatsEntry.FREE_AGENT));

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DialogFragment newFragment = ChangeTeamDialogFragment.newInstance(teams, playerName, firestoreID);
        newFragment.show(fragmentTransaction, "");
    }

    public void deletePlayer() {
        FirestoreHelper firestoreHelper = new FirestoreHelper(getActivity(), mSelectionID);
        if (mCurrentPlayerUri != null) {
            String selection = StatsEntry.COLUMN_FIRESTORE_ID + "=?";
            String[] selectionArgs = new String[]{firestoreID};
            int rowsDeleted = getActivity().getContentResolver().delete(mCurrentPlayerUri, selection, selectionArgs);
            if (rowsDeleted > 0) {
                firestoreHelper.addDeletion(firestoreID, 1, playerName, gender, teamFirestoreID);
                Toast.makeText(getActivity(), playerName + " " + getString(R.string.editor_delete_player_successful), Toast.LENGTH_SHORT).show();
            } else {
                return;
            }
        }
        if (getActivity() instanceof PlayerPagerActivity) {
            ((PlayerPagerActivity) getActivity()).returnDeleteResult(Activity.RESULT_OK, firestoreID);
        }
    }

    public String getFirestoreID() {
        return firestoreID;
    }

    public boolean updatePlayerName(String player) {
        playerName = player;
        ContentValues contentValues = new ContentValues();
        contentValues.put(StatsEntry.COLUMN_NAME, playerName);
        contentValues.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);

        int rowsUpdated = getActivity().getContentResolver().update(mCurrentPlayerUri, contentValues, null, null);
        return rowsUpdated > 0;
    }

    public void updateTeamName(String team) {
        teamString = team;
        ContentValues contentValues = new ContentValues();
        contentValues.put(StatsEntry.COLUMN_TEAM, team);
        contentValues.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);

        getActivity().getContentResolver().update(mCurrentPlayerUri, contentValues, null, null);
    }

    private boolean levelAuthorized(int level) {
        return mLevel >= level;
    }

    public interface OnFragmentInteractionListener {
        void setTeamEdit();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PlayerFragment.OnFragmentInteractionListener) {
            mListener = (PlayerFragment.OnFragmentInteractionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("aaa", "onDestroy() PlayerFragment");
        RefWatcher refWatcher = MyApp.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
