package com.example.android.scorekeepdraft1.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.example.android.scorekeepdraft1.R;
import com.example.android.scorekeepdraft1.data.StatsContract;
import com.example.android.scorekeepdraft1.objects.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChangeTeamDialogFragment extends DialogFragment {

    private List<Team> mTeams;
    private String playerName;
    private String playerFirestoreID;
    private static final String KEY_TEAMS = "teams";
    private static final String KEY_PLAYER_NAME = "playername";
    private static final String KEY_PLAYER_FS_ID = "player_id";
    private OnFragmentInteractionListener mListener;

    public ChangeTeamDialogFragment() {
        // Required empty public constructor
    }

    public static ChangeTeamDialogFragment newInstance(ArrayList<Team> teams,
                                                       String name, String firestoreID) {

        Bundle args = new Bundle();
        args.putParcelableArrayList(KEY_TEAMS, teams);
        args.putString(KEY_PLAYER_NAME, name);
        args.putString(KEY_PLAYER_FS_ID, firestoreID);

        ChangeTeamDialogFragment fragment = new ChangeTeamDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mTeams = args.getParcelableArrayList(KEY_TEAMS);
        playerName = args.getString(KEY_PLAYER_NAME);
        playerFirestoreID = args.getString(KEY_PLAYER_FS_ID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String titleString = getContext().getResources().getString(R.string.edit_player_team);
        String title = String.format(titleString, playerName);

        final Map<String, String> teamMap = new HashMap<>();
        final List<String> teamNames = new ArrayList<>();
        for (Team team : mTeams) {
            teamNames.add(team.getName());
            teamMap.put(team.getName(), team.getFirestoreID());
        }

        final CharSequence[] teams_array = teamNames.toArray(new CharSequence[mTeams.size()]);

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setItems(teams_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String teamName = teams_array[item].toString();
                        String teamID = teamMap.get(teamName);
                        if (teamName.equals(getString(R.string.waivers))) {
                            teamName = StatsContract.StatsEntry.FREE_AGENT;
                        }
                        onButtonPressed(teamName, teamID);
                    }
                })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    public void onButtonPressed(String teamName, String teamID) {
        if (mListener != null) {
            mListener.onTeamChosen(playerFirestoreID, teamName, teamID);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onTeamChosen(String playerID, String teamName, String teamID);
    }
}
