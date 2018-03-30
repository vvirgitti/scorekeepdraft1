package xyz.sleekstats.softball.data;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import xyz.sleekstats.softball.MyApp;
import xyz.sleekstats.softball.objects.Player;
import xyz.sleekstats.softball.data.StatsContract.StatsEntry;
import xyz.sleekstats.softball.objects.PlayerLog;
import xyz.sleekstats.softball.objects.TeamLog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Eddie on 11/7/2017.
 */

public class FirestoreHelper extends IntentService {
    public static final String LEAGUE_COLLECTION = "leagues";
    public static final String PLAYERS_COLLECTION = "players";
    public static final String TEAMS_COLLECTION = "teams";
    public static final String BOXSCORE_COLLECTION = "boxscores";
    public static final String DELETION_COLLECTION = "deletion";
    public static final String PLAYER_LOGS = "playerlogs";
    public static final String BOXSCORE_LOGS = "boxscorelogs";
    public static final String TEAM_LOGS = "teamlogs";
    public static final String LAST_UPDATE = "last_update";
    public static final String UPDATE_SETTINGS = "_updateSettings";
    public static final String USERS = "users";
    public static final String REQUESTS = "requests";

    public static final String STATKEEPER_ID = "statkeeperID";
    public static final String INTENT_ADD_PLAYER_STATS = "addPlayerStats";
    public static final String INTENT_ADD_TEAM_STATS = "addTeamStats";
    public static final String INTENT_DELETE_PLAYER = "delete";
    public static final String INTENT_DELETE_PLAYERS = "deleteList";
    public static final String INTENT_RETRY_GAME_LOAD = "retry";

    private String statKeeperID;
    private Context mContext;
    private FirebaseFirestore mFirestore;

    public FirestoreHelper(String name) {
        super(name);
    }

    //TIMESTAMP MAINTENANCE

    private long getNewTimeStamp() {
        return System.currentTimeMillis();
    }

    private long getLocalTimeStamp() {
//        return 0;
        SharedPreferences updatePreferences = getSharedPreferences(statKeeperID + UPDATE_SETTINGS, Context.MODE_PRIVATE);
        return updatePreferences.getLong(LAST_UPDATE, 0);
    }

    public void setLocalTimeStamp(long time) {
        SharedPreferences updatePreferences = getSharedPreferences(statKeeperID + UPDATE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = updatePreferences.edit();
        editor.putLong(LAST_UPDATE, time);
        editor.apply();
    }

    private long getCloudTimeStamp(DocumentSnapshot documentSnapshot) {
        Map<String, Object> data = documentSnapshot.getData();
        long cloudTimeStamp;
        Object object = data.get(LAST_UPDATE);
        if (object == null) {
            cloudTimeStamp = 0;
            updateCloudTimeStamp(cloudTimeStamp);
        } else {
            cloudTimeStamp = (long) object;
        }
        return cloudTimeStamp;
    }

    public void updateTimeStamps() {

        final long newTimeStamp = getNewTimeStamp();
        final long localTimeStamp = getLocalTimeStamp();

        mFirestore.collection(LEAGUE_COLLECTION).document(statKeeperID).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            long cloudTimeStamp = getCloudTimeStamp(task.getResult());

                            if (localTimeStamp >= cloudTimeStamp) {
                                updateLocalTimeStamp(newTimeStamp);
                            }
                            updateCloudTimeStamp(newTimeStamp);
                        }
                    }
                });

    }

    private void updateLocalTimeStamp(long timestamp) {
        SharedPreferences updatePreferences = getSharedPreferences(statKeeperID + UPDATE_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = updatePreferences.edit();
        editor.putLong(LAST_UPDATE, timestamp);
        editor.apply();
    }

    private void updateCloudTimeStamp(long timestamp) {
        DocumentReference leagueDoc = mFirestore.collection(LEAGUE_COLLECTION).document(statKeeperID);
        leagueDoc.update(LAST_UPDATE, timestamp);
    }


    //SETTING UPDATES

    public void setUpdate(String firestoreID, int type) {
        if (mFirestore == null) {
            mFirestore = FirebaseFirestore.getInstance();
        }

        long timeStamp = System.currentTimeMillis();
        String collection;

        if (type == 0) {
            collection = FirestoreHelper.TEAMS_COLLECTION;
        } else if (type == 1) {
            collection = FirestoreHelper.PLAYERS_COLLECTION;
        } else {
            return;
        }

        DocumentReference documentReference = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION).document(statKeeperID)
                .collection(collection).document(firestoreID);
        documentReference.update(StatsEntry.UPDATE, timeStamp).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(mContext, "UPDATE FAILURE", Toast.LENGTH_LONG).show();
            }
        });
        updateTimeStamps();
    }

    public void addDeletion(final String firestoreID, final int type, final String name, final int gender, final String teamFireID) {
        if (mFirestore == null) {
            mFirestore = FirebaseFirestore.getInstance();
        }
        mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION).document(statKeeperID)
                .collection(FirestoreHelper.PLAYERS_COLLECTION).document(firestoreID)
                .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                setDeletionDoc(statKeeperID, firestoreID, type, name, gender, teamFireID);
            }
        });
    }

    public void addDeletionList(List<Player> playersToDelete) {
        if(playersToDelete.isEmpty()) {
            return;
        }
        if (mFirestore == null) {
            mFirestore = FirebaseFirestore.getInstance();
        }
        CollectionReference playersCollection = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION).document(statKeeperID)
                .collection(FirestoreHelper.PLAYERS_COLLECTION);
        CollectionReference deletionCollection = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION).document(statKeeperID)
                .collection(FirestoreHelper.DELETION_COLLECTION);

        WriteBatch batch = mFirestore.batch();

        for (Player player : playersToDelete) {
            String fireID = player.getFirestoreID();
            String teamFireID = player.getTeamfirestoreid();
            String name = player.getName();
            int gender = player.getGender();
            batch.delete(playersCollection.document(fireID));

            Map<String, Object> deletion = new HashMap<>();
            deletion.put(StatsEntry.TIME, System.currentTimeMillis());
            deletion.put(StatsEntry.TYPE, 1);
            deletion.put(StatsEntry.COLUMN_NAME, name);
            deletion.put(StatsEntry.COLUMN_GENDER, gender);
            deletion.put(StatsEntry.COLUMN_TEAM_FIRESTORE_ID, teamFireID);

            batch.set(deletionCollection.document(fireID), deletion, SetOptions.merge());
        }
        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateTimeStamps();
            }
        });
    }

    private void setDeletionDoc(String leagueID, String firestoreID, int type, String name, int gender, String team) {
        if (mFirestore == null) {
            mFirestore = FirebaseFirestore.getInstance();
        }

        DocumentReference deletionDoc = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION).document(leagueID)
                .collection(FirestoreHelper.DELETION_COLLECTION).document(firestoreID);

        Map<String, Object> deletion = new HashMap<>();
        long time = System.currentTimeMillis();
        deletion.put(StatsEntry.TIME, time);
        deletion.put(StatsEntry.TYPE, type);
        deletion.put(StatsEntry.COLUMN_NAME, name);
        if(type == 1) {
            deletion.put(StatsEntry.COLUMN_GENDER, gender);
            deletion.put(StatsEntry.COLUMN_TEAM_FIRESTORE_ID, team);
        }

        deletionDoc.set(deletion, SetOptions.merge());
        updateTimeStamps();
    }

    public void addPlayerStatsToDB(final long gameID) {

        ArrayList<Long> playerList = new ArrayList<>();
        String selection = StatsEntry.COLUMN_PLAYERID + "=?";

        Cursor cursor = getContentResolver().query(StatsEntry.CONTENT_URI_TEMP, null,
                null, null, null);

        while (cursor.moveToNext()) {
            long playerId = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_PLAYERID);
            playerList.add(playerId);
        }

        WriteBatch playerBatch = mFirestore.batch();

        for (long playerId : playerList) {
            String[] selectionArgs = new String[]{String.valueOf(playerId)};

            cursor = getContentResolver().query(StatsEntry.CONTENT_URI_TEMP, null,
                    selection, selectionArgs, null);
            cursor.moveToFirst();
            int gameRBI = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RBI);
            int gameRun = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUN);
            int game1b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_1B);
            int game2b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_2B);
            int game3b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_3B);
            int gameHR = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_HR);
            int gameOuts = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_OUT);
            int gameBB = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_BB);
            int gameSF = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_SF);
            String firestoreID = StatsContract.getColumnString(cursor, StatsEntry.COLUMN_FIRESTORE_ID);

            Map<String, Object> boxscoreMap = new HashMap<>();
            boxscoreMap.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);
            boxscoreMap.put(StatsEntry.COLUMN_GAME_ID, gameID);
            boxscoreMap.put(StatsEntry.COLUMN_1B, game1b);
            boxscoreMap.put(StatsEntry.COLUMN_2B, game2b);
            boxscoreMap.put(StatsEntry.COLUMN_3B, game3b);
            boxscoreMap.put(StatsEntry.COLUMN_HR, gameHR);
            boxscoreMap.put(StatsEntry.COLUMN_RUN, gameRun);
            boxscoreMap.put(StatsEntry.COLUMN_RBI, gameRBI);
            boxscoreMap.put(StatsEntry.COLUMN_BB, gameBB);
            boxscoreMap.put(StatsEntry.COLUMN_OUT, gameOuts);
            boxscoreMap.put(StatsEntry.COLUMN_SF, gameSF);
            final DocumentReference boxscoreRef = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION)
                    .document(statKeeperID).collection(FirestoreHelper.BOXSCORE_COLLECTION).document(String.valueOf(gameID))
                    .collection(BOXSCORE_LOGS).document(firestoreID);
            playerBatch.set(boxscoreRef, boxscoreMap, SetOptions.merge());

            ContentValues boxscoreValues = new ContentValues();
            boxscoreValues.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);
            boxscoreValues.put(StatsEntry.COLUMN_GAME_ID, gameID);
            boxscoreValues.put(StatsEntry.COLUMN_1B, game1b);
            boxscoreValues.put(StatsEntry.COLUMN_2B, game2b);
            boxscoreValues.put(StatsEntry.COLUMN_3B, game3b);
            boxscoreValues.put(StatsEntry.COLUMN_HR, gameHR);
            boxscoreValues.put(StatsEntry.COLUMN_RUN, gameRun);
            boxscoreValues.put(StatsEntry.COLUMN_RBI, gameRBI);
            boxscoreValues.put(StatsEntry.COLUMN_BB, gameBB);
            boxscoreValues.put(StatsEntry.COLUMN_OUT, gameOuts);
            boxscoreValues.put(StatsEntry.COLUMN_SF, gameSF);
            getContentResolver().insert(StatsEntry.CONTENT_URI_BOXSCORES, boxscoreValues);



            long logId;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                logId = new Date().getTime();
            } else {
                logId = System.currentTimeMillis();
            }

            final DocumentReference playerRef = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION)
                    .document(statKeeperID).collection(FirestoreHelper.PLAYERS_COLLECTION).document(firestoreID)
                    .collection(FirestoreHelper.PLAYER_LOGS).document(String.valueOf(logId));

            PlayerLog playerLog = new PlayerLog(playerId, gameRBI, gameRun, game1b, game2b, game3b,
                    gameHR, gameOuts, gameBB, gameSF);
            playerBatch.set(playerRef, playerLog);

            Uri playerUri = ContentUris.withAppendedId(StatsEntry.CONTENT_URI_PLAYERS, playerId);
            cursor = getContentResolver().query(playerUri, null, null, null, null);
            cursor.moveToFirst();

            int pRBI = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RBI);
            int pRun = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUN);
            int p1b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_1B);
            int p2b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_2B);
            int p3b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_3B);
            int pHR = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_HR);
            int pOuts = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_OUT);
            int pBB = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_BB);
            int pSF = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_SF);
            int games = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_G);
            firestoreID = StatsContract.getColumnString(cursor, StatsEntry.COLUMN_FIRESTORE_ID);

            ContentValues values = new ContentValues();
            values.put(StatsEntry.COLUMN_1B, p1b + game1b);
            values.put(StatsEntry.COLUMN_2B, p2b + game2b);
            values.put(StatsEntry.COLUMN_3B, p3b + game3b);
            values.put(StatsEntry.COLUMN_HR, pHR + gameHR);
            values.put(StatsEntry.COLUMN_RUN, pRun + gameRun);
            values.put(StatsEntry.COLUMN_RBI, pRBI + gameRBI);
            values.put(StatsEntry.COLUMN_BB, pBB + gameBB);
            values.put(StatsEntry.COLUMN_OUT, pOuts + gameOuts);
            values.put(StatsEntry.COLUMN_SF, pSF + gameSF);
            values.put(StatsEntry.COLUMN_G, games + 1);
            values.put(StatsEntry.COLUMN_FIRESTORE_ID, firestoreID);
            getContentResolver().update(playerUri, values, null, null);
            setUpdate(firestoreID, 1);
        }
        cursor.close();
        playerBatch.commit().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Cursor cursor = getContentResolver().query(StatsEntry.CONTENT_URI_TEMP, null,
                        null, null, null);
                while (cursor.moveToNext()) {
                    Log.d("xyxyx", "playerBatch.commit( faill" + e.toString());
                    long logId;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        logId = new Date().getTime();
                    } else {
                        logId = System.currentTimeMillis();
                    }
                    int playerId = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_PLAYERID);
                    int gameRBI = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RBI);
                    int gameRun = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUN);
                    int game1b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_1B);
                    int game2b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_2B);
                    int game3b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_3B);
                    int gameHR = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_HR);
                    int gameOuts = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_OUT);
                    int gameBB = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_BB);
                    int gameSF = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_SF);
                    String playerFirestoreID = StatsContract.getColumnString(cursor, StatsEntry.COLUMN_FIRESTORE_ID);

                    ContentValues backupValues = new ContentValues();
                    backupValues.put(StatsEntry.COLUMN_FIRESTORE_ID, playerFirestoreID);
                    backupValues.put(StatsEntry.COLUMN_1B, game1b);
                    backupValues.put(StatsEntry.COLUMN_2B, game2b);
                    backupValues.put(StatsEntry.COLUMN_3B, game3b);
                    backupValues.put(StatsEntry.COLUMN_HR, gameHR);
                    backupValues.put(StatsEntry.COLUMN_RUN, gameRun);
                    backupValues.put(StatsEntry.COLUMN_RBI, gameRBI);
                    backupValues.put(StatsEntry.COLUMN_BB, gameBB);
                    backupValues.put(StatsEntry.COLUMN_OUT, gameOuts);
                    backupValues.put(StatsEntry.COLUMN_SF, gameSF);

                    backupValues.put(StatsEntry.COLUMN_GAME_ID, gameID);
                    getContentResolver().insert(StatsEntry.CONTENT_URI_BACKUP_BOXSCORES, backupValues);

                    backupValues.remove(StatsEntry.COLUMN_GAME_ID);
                    backupValues.put(StatsEntry.COLUMN_LOG_ID, logId);
                    backupValues.put(StatsEntry.COLUMN_PLAYERID, playerId);
                    getContentResolver().insert(StatsEntry.CONTENT_URI_BACKUP_PLAYERS, backupValues);
                }
                cursor.close();
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("xyxyx", "playerBatch.commit( SUCCESS");
            }
        });
    }

    public void addTeamStatsToDB(final long gameID, final String teamFirestoreID, int teamRuns, int otherTeamRuns) {
        WriteBatch teamBatch = mFirestore.batch();

        String selection = StatsEntry.COLUMN_FIRESTORE_ID + "=?";
        String[] selectionArgs = {teamFirestoreID};
        Cursor cursor = getContentResolver().query(StatsEntry.CONTENT_URI_TEAMS, null,
                selection, selectionArgs, null
        );
        cursor.moveToFirst();
        ContentValues values = new ContentValues();
        final ContentValues backupValues = new ContentValues();

        long logId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            logId = new Date().getTime();
        } else {
            logId = System.currentTimeMillis();
        }

        Map<String, Object> boxscoreMap = new HashMap<>();
        boxscoreMap.put(StatsEntry.COLUMN_FIRESTORE_ID, teamFirestoreID);
        boxscoreMap.put(StatsEntry.COLUMN_GAME_ID, gameID);
        boxscoreMap.put(StatsEntry.COLUMN_1B, -1);
        boxscoreMap.put(StatsEntry.COLUMN_2B, teamRuns);
        boxscoreMap.put(StatsEntry.COLUMN_3B, otherTeamRuns);

        final DocumentReference boxscoreRef = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION)
                .document(statKeeperID).collection(FirestoreHelper.BOXSCORE_COLLECTION).document(String.valueOf(gameID))
                .collection(BOXSCORE_LOGS).document(teamFirestoreID);
        teamBatch.set(boxscoreRef, boxscoreMap, SetOptions.merge());

        final ContentValues boxscoreValues = new ContentValues();
        boxscoreValues.put(StatsEntry.COLUMN_FIRESTORE_ID, teamFirestoreID);
        boxscoreValues.put(StatsEntry.COLUMN_GAME_ID, gameID);
        boxscoreValues.put(StatsEntry.COLUMN_1B, -1);
        boxscoreValues.put(StatsEntry.COLUMN_2B, teamRuns);
        boxscoreValues.put(StatsEntry.COLUMN_3B, otherTeamRuns);
        getContentResolver().insert(StatsEntry.CONTENT_URI_BOXSCORES, boxscoreValues);



        long teamId = StatsContract.getColumnLong(cursor, StatsEntry._ID);
        TeamLog teamLog = new TeamLog(teamId, teamRuns, otherTeamRuns);
        backupValues.put(StatsEntry.COLUMN_TEAM_ID, teamId);

        final DocumentReference docRef = mFirestore.collection(FirestoreHelper.LEAGUE_COLLECTION)
                .document(statKeeperID).collection(FirestoreHelper.TEAMS_COLLECTION).document(teamFirestoreID)
                .collection(FirestoreHelper.TEAM_LOGS).document(String.valueOf(logId));

        if (teamRuns > otherTeamRuns) {
            int newValue = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_WINS) + 1;
            values.put(StatsEntry.COLUMN_WINS, newValue);
            backupValues.put(StatsEntry.COLUMN_WINS, 1);
            teamLog.setWins(1);
        } else if (otherTeamRuns > teamRuns) {
            int newValue = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_LOSSES) + 1;
            values.put(StatsEntry.COLUMN_LOSSES, newValue);
            backupValues.put(StatsEntry.COLUMN_LOSSES, 1);
            teamLog.setLosses(1);
        } else {
            int newValue = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_TIES) + 1;
            values.put(StatsEntry.COLUMN_TIES, newValue);
            backupValues.put(StatsEntry.COLUMN_TIES, 1);
            teamLog.setTies(1);
        }

        int newValue = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUNSFOR) + teamRuns;
        values.put(StatsEntry.COLUMN_RUNSFOR, newValue);
        backupValues.put(StatsEntry.COLUMN_RUNSFOR, teamRuns);

        newValue = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUNSAGAINST) + otherTeamRuns;
        values.put(StatsEntry.COLUMN_RUNSAGAINST, newValue);
        backupValues.put(StatsEntry.COLUMN_RUNSAGAINST, otherTeamRuns);
        cursor.close();

        values.put(StatsEntry.COLUMN_FIRESTORE_ID, teamFirestoreID);

        getContentResolver().update(StatsEntry.CONTENT_URI_TEAMS, values, selection, selectionArgs);

        teamBatch.set(docRef, teamLog);
        teamBatch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                setUpdate(teamFirestoreID, 0);
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        getContentResolver().insert(StatsEntry.CONTENT_URI_BACKUP_TEAMS, backupValues);
                        getContentResolver().insert(StatsEntry.CONTENT_URI_BACKUP_BOXSCORES, boxscoreValues);
                    }
                });

    }



    public void retryGameLogLoad() {
        MyApp myApp = (MyApp) getApplicationContext();
        String leagueID = myApp.getCurrentSelection().getId();

        WriteBatch batch = mFirestore.batch();

        Cursor cursor = getContentResolver().query(StatsEntry.CONTENT_URI_BACKUP_PLAYERS,
                null, null, null, null);
        while (cursor.moveToNext()) {
            String playerFirestoreID = StatsContract.getColumnString(cursor, StatsEntry.COLUMN_FIRESTORE_ID);
            long logId = StatsContract.getColumnLong(cursor, StatsEntry.COLUMN_LOG_ID);
            long playerId = StatsContract.getColumnLong(cursor, StatsEntry.COLUMN_PLAYERID);
            int gameRBI = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RBI);
            int gameRun = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUN);
            int game1b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_1B);
            int game2b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_2B);
            int game3b = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_3B);
            int gameHR = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_HR);
            int gameOuts = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_OUT);
            int gameBB = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_BB);
            int gameSF = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_SF);

            final DocumentReference docRef = mFirestore.collection(LEAGUE_COLLECTION).document(leagueID).collection(PLAYERS_COLLECTION)
                    .document(playerFirestoreID).collection(PLAYER_LOGS).document(String.valueOf(logId));

            PlayerLog playerLog = new PlayerLog(playerId, gameRBI, gameRun, game1b, game2b, game3b, gameHR, gameOuts, gameBB, gameSF);
            batch.set(docRef, playerLog);
        }

        cursor.close();

        cursor = getContentResolver().query(StatsEntry.CONTENT_URI_BACKUP_TEAMS, null,
                null, null, null);
        while (cursor.moveToNext()) {
            long logId = StatsContract.getColumnLong(cursor, StatsEntry.COLUMN_LOG_ID);
            long teamId = StatsContract.getColumnLong(cursor, StatsEntry.COLUMN_TEAM_ID);
            int gameWins = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_WINS);
            int gameLosses = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_LOSSES);
            int gameTies = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_TIES);
            int gameRunsScored = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUNSFOR);
            int gameRunsAllowed = StatsContract.getColumnInt(cursor, StatsEntry.COLUMN_RUNSAGAINST);
            String teamFirestoreID = StatsContract.getColumnString(cursor, StatsEntry.COLUMN_FIRESTORE_ID);

            final DocumentReference docRef = mFirestore.collection(LEAGUE_COLLECTION).document(leagueID).collection(TEAMS_COLLECTION)
                    .document(teamFirestoreID).collection(TEAM_LOGS).document(String.valueOf(logId));

            TeamLog teamLog = new TeamLog(teamId, gameWins, gameLosses, gameTies, gameRunsScored, gameRunsAllowed);
            batch.set(docRef, teamLog);
        }

        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                getContentResolver().delete(StatsEntry.CONTENT_URI_BACKUP_PLAYERS, null, null);
                getContentResolver().delete(StatsEntry.CONTENT_URI_BACKUP_TEAMS, null, null);
            }
        });
        cursor.close();
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent == null) {return;}
        String action = intent.getAction();
        if(action == null) {return;}
        if(mFirestore == null) {
            mFirestore = FirebaseFirestore.getInstance();
        }
        statKeeperID = intent.getStringExtra(STATKEEPER_ID);
        long gameID;
        String firestoreID;
        switch (action) {
            case INTENT_ADD_PLAYER_STATS:
                gameID = intent.getLongExtra(StatsEntry.COLUMN_GAME_ID, 0);
                addPlayerStatsToDB(gameID);
                break;

            case INTENT_ADD_TEAM_STATS:
                gameID = intent.getLongExtra(StatsEntry.COLUMN_GAME_ID, 0);
                firestoreID = intent.getStringExtra(StatsEntry.COLUMN_FIRESTORE_ID);
                int runsFor = intent.getIntExtra(StatsEntry.COLUMN_RUNSFOR, 0);
                int runsAgainst = intent.getIntExtra(StatsEntry.COLUMN_RUNSAGAINST, 0);
                addTeamStatsToDB(gameID, firestoreID, runsFor, runsAgainst);
                break;

            case INTENT_RETRY_GAME_LOAD:
                retryGameLogLoad();
                break;

            case INTENT_DELETE_PLAYER:
                firestoreID = intent.getStringExtra(StatsEntry.COLUMN_FIRESTORE_ID);
                String teamFirestoreID = intent.getStringExtra(StatsEntry.COLUMN_TEAM_FIRESTORE_ID);
                int type = intent.getIntExtra(StatsEntry.TYPE, -1);
                String name = intent.getStringExtra(StatsEntry.COLUMN_NAME);
                int gender = intent.getIntExtra(StatsEntry.COLUMN_GENDER, -1);
                addDeletion(firestoreID, type, name, gender, teamFirestoreID);
                break;

            case INTENT_DELETE_PLAYERS:
                List<Player> playersToDelete = intent.getParcelableArrayListExtra("playersToDelete");
                addDeletionList(playersToDelete);
                break;
        }
    }
}