package xyz.sleekstats.softball.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import xyz.sleekstats.softball.R;

public class RemoveAllPlayersDialog extends DialogFragment {


    private OnFragmentInteractionListener mListener;
    private String objectToRemove;
    private boolean isLeague;
    private boolean isWaivers;
    private static final int CHOICE_CANCEL = 0;
    private static final int CHOICE_WAIVERS = 1;
    private static final int CHOICE_DELETE = 2;

    public RemoveAllPlayersDialog() {
        // Required empty public constructor
    }

    public static RemoveAllPlayersDialog newInstance(String objectToRemove, boolean league, boolean waivers) {

        Bundle args = new Bundle();
        RemoveAllPlayersDialog fragment = new RemoveAllPlayersDialog();
        args.putString("objectToRemove", objectToRemove);
        args.putBoolean("isLeague", league);
        args.putBoolean("isWaivers", waivers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            objectToRemove = args.getString("objectToRemove");
            isLeague = args.getBoolean("isLeague");
            isWaivers = args.getBoolean("isWaivers");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (isLeague) {
            if (isWaivers) {
                builder.setMessage(R.string.remove_all_free_agents);
            } else {
                String message = String.format(getString(R.string.send_all_to_waivers), objectToRemove);
                builder.setMessage(message);
            }

            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (isWaivers) {
                        onButtonPressed(CHOICE_DELETE);
                    } else {
                        onButtonPressed(CHOICE_WAIVERS);
                    }
                }
            });
        } else {
            builder.setTitle("Delete all players?")
                    .setMessage("Players and their stats will be permanently deleted.")
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onButtonPressed(CHOICE_DELETE);
                        }
                    });
        }
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onButtonPressed(CHOICE_CANCEL);
            }
        });

        return builder.create();
    }


    private void onButtonPressed(int choice) {
        if (mListener != null) {
            mListener.onRemoveChoice(choice);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLineupSortListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onRemoveChoice(int choice);
    }
}
