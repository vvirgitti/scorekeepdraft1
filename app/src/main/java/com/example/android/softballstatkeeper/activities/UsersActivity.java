package com.example.android.softballstatkeeper.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.softballstatkeeper.MyApp;
import com.example.android.softballstatkeeper.R;
import com.example.android.softballstatkeeper.adapters.UserListAdapter;
import com.example.android.softballstatkeeper.data.StatsContract;
import com.example.android.softballstatkeeper.dialogs.EmailInviteDialog;
import com.example.android.softballstatkeeper.dialogs.InviteUserDialog;
import com.example.android.softballstatkeeper.models.MainPageSelection;
import com.example.android.softballstatkeeper.models.StatKeepUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.android.softballstatkeeper.data.FirestoreHelper.LEAGUE_COLLECTION;
import static com.example.android.softballstatkeeper.data.FirestoreHelper.REQUESTS;
import static com.example.android.softballstatkeeper.data.FirestoreHelper.USERS;

public class UsersActivity extends AppCompatActivity
        implements InviteUserDialog.OnFragmentInteractionListener,
        EmailInviteDialog.OnListFragmentInteractionListener,
        UserListAdapter.AdapterListener {

    private static final String TAG = "UsersActivity";
    private static final String SAVED_MAP = "map";
    private static final String SAVED_USER_LEVELS = "userlevels";
    private static final String SAVED_CREATOR = "creator";

    public static final int LEVEL_REMOVE_USER = 0;
    public static final int LEVEL_VIEW_ONLY = 1;
    public static final int LEVEL_VIEW_WRITE = 2;
    public static final int LEVEL_ADMIN = 3;
    public static final int LEVEL_CREATOR = 4;

    private List<StatKeepUser> mUserList;
    private HashMap<String, Integer> mOriginalLevelsMap;
    private HashMap<String, Integer> levelChanges;
    private StatKeepUser creator;

    private RecyclerView mRecyclerView;
    private UserListAdapter mAdapter;

    private Button startAdderBtn;
    private Button saveBtn;
    private Button resetBtn;

    private String mSelectionID;
    private String mSelectionName;
    private int mSelectionType;
    private int mLevel;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        try {
            MyApp myApp = (MyApp) getApplicationContext();
            MainPageSelection mainPageSelection = myApp.getCurrentSelection();
            mSelectionID = mainPageSelection.getId();
            mSelectionName = mainPageSelection.getName();
            mSelectionType = mainPageSelection.getType();
            mLevel = mainPageSelection.getLevel();
            setTitle(mSelectionName);
            TextView leagueNameTextView = findViewById(R.id.league_name_display);
            String leagueNameDisplay = mSelectionName + " Users";
            leagueNameTextView.setText(leagueNameDisplay);
        } catch (Exception e) {
            Intent intent = new Intent(UsersActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        mRecyclerView = findViewById(R.id.rv_users);
        firestore = FirebaseFirestore.getInstance();

        setButtons();

        if (savedInstanceState != null) {
            levelChanges = (HashMap<String, Integer>) savedInstanceState.getSerializable(SAVED_MAP);
            mOriginalLevelsMap = (HashMap<String, Integer>) savedInstanceState.getSerializable(SAVED_USER_LEVELS);
            creator = savedInstanceState.getParcelable(SAVED_CREATOR);
            setCreator(creator);
            return;
        }

        firestore.collection(LEAGUE_COLLECTION).document(mSelectionID).collection(USERS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            mOriginalLevelsMap = new HashMap<>();
                            mUserList = new ArrayList<>();

                            for (DocumentSnapshot document : task.getResult()) {

                                StatKeepUser statKeepUser = document.toObject(StatKeepUser.class);
                                statKeepUser.setId(document.getId());
                                int level = statKeepUser.getLevel();

                                if (level == LEVEL_CREATOR) {
                                    creator = statKeepUser;
                                    setCreator(statKeepUser);
                                } else if (level > 0 && level < LEVEL_CREATOR) {
                                    mOriginalLevelsMap.put(statKeepUser.getEmail(), statKeepUser.getLevel());
                                    mUserList.add(statKeepUser);
                                }
                            }
                            Collections.sort(mUserList, StatKeepUser.levelComparator());
                            updateRV();

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void updateRV() {
//        if (mAdapter == null) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(
                    this, LinearLayoutManager.VERTICAL, false));
            mAdapter = new UserListAdapter(mUserList, this, mLevel);
            mRecyclerView.setAdapter(mAdapter);
//        }
//        else {
//            mAdapter.notifyDataSetChanged();
//        }
    }

    private void setCreator(StatKeepUser statKeepUser) {
        String creatorEmail = statKeepUser.getEmail();

        TextView nameView = findViewById(R.id.admin_name_view);
        TextView emailView = findViewById(R.id.admin_email_view);
        nameView.setText(statKeepUser.getName());
        emailView.setText(creatorEmail);
    }

    private void openInviteUserDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DialogFragment newFragment = new InviteUserDialog();
        newFragment.show(fragmentTransaction, "");
    }

    private void openEmailInvitesDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DialogFragment newFragment = new EmailInviteDialog();
        newFragment.show(fragmentTransaction, "");
    }

    private void setButtons() {
        if (mLevel >= LEVEL_ADMIN) {
            startAdderBtn = findViewById(R.id.btn_start_adder);
            startAdderBtn.setVisibility(View.VISIBLE);
            startAdderBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startAdderBtn.setVisibility(View.INVISIBLE);
                    openInviteUserDialog();
                }
            });

            saveBtn = findViewById(R.id.btn_save_changes);
            resetBtn = findViewById(R.id.btn_reset);
            saveBtn.setVisibility(View.INVISIBLE);
            resetBtn.setVisibility(View.INVISIBLE);
        }
    }

    public void saveChanges(View v) {
        if (levelChanges == null) {
            return;
        }
        WriteBatch batch = firestore.batch();
        for (Map.Entry<String, Integer> entry : levelChanges.entrySet()) {
            String id = entry.getKey();
            int level = entry.getValue();
            DocumentReference league = firestore.collection(LEAGUE_COLLECTION).document(mSelectionID);
            DocumentReference leagueUser = firestore.collection(LEAGUE_COLLECTION).document(mSelectionID)
                    .collection(USERS).document(id);
            if (level == LEVEL_REMOVE_USER) {
                batch.update(league, id, 0);
                batch.delete(leagueUser);
            } else {
                batch.update(league, id, level);
                batch.update(leagueUser, StatsContract.StatsEntry.LEVEL, level);
            }
        }
        batch.commit();
        onBackPressed();
    }

    public void resetChanges(View v) {

        saveBtn.setVisibility(View.INVISIBLE);
        resetBtn.setVisibility(View.INVISIBLE);
        if (levelChanges == null) {
            return;
        }
        levelChanges.clear();
        revertUserList();
        updateRV();
    }

    private void revertUserList() {
        for (StatKeepUser user : mUserList) {
            String email = user.getEmail();

            int oldLevel = mOriginalLevelsMap.get(email);
            user.setLevel(oldLevel);
        }
    }

    public void sendEmailUpdate(View view) {
        int userSize = mUserList.size();
        List<String> users = new ArrayList<>();
        for (int i = 0; i < userSize; i++) {
            StatKeepUser statKeepUser = mUserList.get(i);
            String user = statKeepUser.getEmail();
            users.add(user);
        }

        String[] emailList = new String[userSize];
        emailList = users.toArray(emailList);

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, mSelectionName + " Update");
        emailIntent.putExtra(Intent.EXTRA_BCC, emailList);
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && data != null) {
            Uri contact = data.getData();
            Log.d("zizi", contact.toString() + " - ");

            String[] projection = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
            while (cursor.moveToNext()) {
                String name = StatsContract.getColumnString(cursor, ContactsContract.Contacts.DISPLAY_NAME);
                Log.d("zizi", name + " - ");
            }
        }
    }

    public void onUserLevelChanged(String name, int level) {
        if (levelChanges == null) {
            levelChanges = new HashMap<>();
        }
        levelChanges.put(name, level);
        saveBtn.setVisibility(View.VISIBLE);
        resetBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (levelChanges != null) {
            outState.putSerializable(SAVED_MAP, levelChanges);
        }
        outState.putSerializable(SAVED_USER_LEVELS, mOriginalLevelsMap);
        outState.putParcelable(SAVED_CREATOR, creator);
    }

    @Override
    public void onEmailInvites() {
        openEmailInvitesDialog();
    }

    @Override
    public void onInviteUsers() {

        DocumentReference documentReference = firestore.collection(LEAGUE_COLLECTION)
                .document(mSelectionID).collection(REQUESTS).document();

        StatKeepUser statKeepUser = new StatKeepUser(REQUESTS, mSelectionName, String.valueOf(mSelectionType), UsersActivity.LEVEL_VIEW_ONLY - 100);
        documentReference.set(statKeepUser, SetOptions.merge());

        String selectionType;
        if (mSelectionType == MainPageSelection.TYPE_LEAGUE) {
            selectionType = "League";
        } else {
            selectionType = "Team";
        }

        Intent msgIntent = new Intent(Intent.ACTION_SEND);
        msgIntent.setType("text/plain");
        msgIntent.putExtra(Intent.EXTRA_TEXT, "You have been granted access to view the stats & standings for "
                + mSelectionName + "!n\n Click on \"Join " + selectionType
                + "\" and enter the following code: " + mSelectionID + "-" + documentReference.getId());
        startActivity(Intent.createChooser(msgIntent, "Message invite code to friends!"));

        startAdderBtn.setVisibility(View.VISIBLE);

//
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, "You have been invited to be a StatKeeper for "
//                + mSelectionName + "!\n Click on \"Join " + selectionType + "\" and enter the " +
//                "StatKeeper ID and Code. This code can only be used once.");
//        startActivity(Intent.createChooser(shareIntent, "Share link using"));
    }


    @Override
    public void onCancel() {
        startAdderBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSubmitEmails(List<String> emails, List<Integer> levels) {
        int emailSize = emails.size();
        if (emailSize < 1) {
            return;
        }

        for (int i = 0; i < emailSize; i++) {
            final String email = emails.get(i);
            final int level = levels.get(i);

            firestore.collection(USERS).whereEqualTo(StatsContract.StatsEntry.EMAIL, email)
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                        if (!documentSnapshots.isEmpty()) {
                            DocumentSnapshot documentSnapshot = documentSnapshots.get(0);
                            String userID = documentSnapshot.getId();

                            Map<String, Object> data = new HashMap<>();
                            data.put(StatsContract.StatsEntry.EMAIL, email);
                            data.put(StatsContract.StatsEntry.COLUMN_NAME, null);
                            data.put(StatsContract.StatsEntry.LEVEL, level);
                            firestore.collection(LEAGUE_COLLECTION).document(mSelectionID)
                                    .collection(USERS).document(userID).set(data, SetOptions.merge());

                            Map<String, Integer> data2 = new HashMap<>();
                            data2.put(userID, -level);
                            firestore.collection(LEAGUE_COLLECTION).document(mSelectionID)
                                    .set(data2, SetOptions.merge());
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            });
        }

        String[] emailList = new String[emailSize];
        emailList = emails.toArray(emailList);

        //todo add link
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "", null));
        emailIntent.putExtra(Intent.EXTRA_BCC, emailList);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "You have been invited to be a StatKeeper for " + mSelectionName + "!");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "You have been invited to view, manage, and share stats and standings for " + mSelectionName + "." +
                "\n\nFollow this link to begin: ");
        startActivity(Intent.createChooser(emailIntent, "Email friends about their invitation!"));

        startAdderBtn.setVisibility(View.VISIBLE);
    }

}
