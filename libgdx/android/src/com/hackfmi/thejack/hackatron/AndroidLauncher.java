package com.hackfmi.thejack.hackatron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.example.games.basegameutils.BaseGameUtils;
import com.hackfmi.thejack.hackatron.Game.BodyPart;

public class AndroidLauncher extends Activity implements View.OnClickListener,
    ConnectionHandler.ChangeListener, SensorEventListener {
  final static String TAG = "ButtonClicker2000";

  // Request codes for the UIs that we show with startActivityForResult:
  final static int RC_SELECT_PLAYERS = 10000;
  final static int RC_INVITATION_INBOX = 10001;
  final static int RC_WAITING_ROOM = 10002;

  // Request code used to invoke sign in user interactions.
  private static final int RC_SIGN_IN = 9001;

  // Are we playing in multiplayer mode?
  boolean mMultiplayer = false;

  // The participants in the currently active game
  ArrayList<Participant> mParticipants = null;

  // My participant ID in the currently active game
  String mMyId = null;

  // If non-null, this is the id of the invitation we received via the
  // invitation listener
  String mIncomingInvitationId = null;

  // Message buffer for sending messages
  byte[] mMsgBuf = new byte[2];

  // Game
  private Game currentGame;

  // Connection
  private ConnectionHandler connectionHandler;

  // Sensors
  private SensorManager mSensorManager;
  private Sensor mSensor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.android_launcher);

    connectionHandler = new ConnectionHandler(this);
    connectionHandler.registerListener(this);
    // set up a click listener for everything we care about
    for (int id : CLICKABLES) {
      findViewById(id).setOnClickListener(this);
    }

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    mParticipants = new ArrayList<Participant>();
  }

  private void useBodyPart(BodyPart bodyPart) {
    currentGame.use(bodyPart);
  }

  @Override
  public void onClick(View v) {
    Intent intent;

    switch (v.getId()) {
    case R.id.button_single_player:
    case R.id.button_single_player_2:
      // play a single-player game
      resetGameVars();
      startGame(false);
      break;
    case R.id.button_sign_in:
      // start the sign-in flow
      if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
        Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
      }
      Log.d(TAG, "Sign-in button clicked");
      connectionHandler.mSignInClicked = true;
      connectionHandler.connect();
      break;
    case R.id.button_sign_out:
      // user wants to sign out
      // sign out.
      Log.d(TAG, "Sign-out button clicked");
      connectionHandler.mSignInClicked = false;
      connectionHandler.disconnect();
      switchToScreen(R.id.screen_sign_in);
      break;
    case R.id.button_invite_players:
      // show list of invitable players
      intent = connectionHandler.getSelectOpponentsIntent();
      switchToScreen(R.id.screen_wait);
      startActivityForResult(intent, RC_SELECT_PLAYERS);
      break;
    case R.id.button_see_invitations:
      // show list of pending invitations
      intent = connectionHandler.getInvitationInboxIntent();
      switchToScreen(R.id.screen_wait);
      startActivityForResult(intent, RC_INVITATION_INBOX);
      break;
    case R.id.button_accept_popup_invitation:
      // user wants to accept the invitation shown on the invitation popup
      // (the one we got through the OnInvitationReceivedListener).
      acceptInviteToRoom(mIncomingInvitationId);
      mIncomingInvitationId = null;
      break;
    case R.id.button_quick_game:
      // user wants to play against a random opponent right now
      switchToScreen(R.id.screen_wait);
      keepScreenOn();
      resetGameVars();
      connectionHandler.startQuickGame();
      break;
    case R.id.button_head:
      useBodyPart(Game.BodyPart.HEAD);
      break;
    case R.id.button_left_hand:
      useBodyPart(Game.BodyPart.LEFT_HAND);
      break;
    case R.id.button_right_hand:
      useBodyPart(Game.BodyPart.RIGHT_HAND);
      break;
    case R.id.button_left_foot:
      useBodyPart(Game.BodyPart.LEFT_FOOT);
      break;
    case R.id.button_right_foot:
      useBodyPart(Game.BodyPart.RIGHT_FOOT);
      break;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int responseCode, Intent intent) {
    super.onActivityResult(requestCode, responseCode, intent);

    switch (requestCode) {
    case RC_SELECT_PLAYERS:
      // we got the result from the "select players" UI -- ready to create
      // the room
      handleSelectPlayersResult(responseCode, intent);
      break;
    case RC_INVITATION_INBOX:
      // we got the result from the "select invitation" UI (invitation
      // inbox). We're
      // ready to accept the selected invitation:
      handleInvitationInboxResult(responseCode, intent);
      break;
    case RC_WAITING_ROOM:
      // we got the result from the "waiting room" UI.
      if (responseCode == Activity.RESULT_OK) {
        // ready to start playing
        Log.d(TAG, "Starting game (waiting room returned OK).");
        startGame(true);
      } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
        // player indicated that they want to leave the room
        leaveRoom();
      } else if (responseCode == Activity.RESULT_CANCELED) {
        // Dialog was cancelled (user pressed back key, for instance).
        // In our game,
        // this means leaving the room too. In more elaborate games,
        // this could mean
        // something else (like minimizing the waiting room UI).
        leaveRoom();
      }
      break;
    case RC_SIGN_IN:
      Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode=" + responseCode
          + ", intent=" + intent);
      connectionHandler.mSignInClicked = false;
      connectionHandler.mResolvingConnectionFailure = false;
      if (responseCode == RESULT_OK) {
        connectionHandler.connect();
      } else {
        BaseGameUtils.showActivityResultError(this, requestCode, responseCode,
            R.string.signin_failure, R.string.signin_other_error);
      }
      break;
    }
    super.onActivityResult(requestCode, responseCode, intent);
  }

  // Handle the result of the "Select players UI" we launched when the user
  // clicked the
  // "Invite friends" button. We react by creating a room with those players.
  private void handleSelectPlayersResult(int response, Intent data) {
    if (response != Activity.RESULT_OK) {
      Log.w(TAG, "*** select players UI cancelled, " + response);
      switchToMainScreen();
      return;
    }

    Log.d(TAG, "Select players UI succeeded.");

    // get the invitee list
    final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
    Log.d(TAG, "Invitee count: " + invitees.size());

    // get the automatch criteria
    int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
    int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

    // create the room
    Log.d(TAG, "Creating room...");
    connectionHandler.createRoom(invitees, minAutoMatchPlayers, maxAutoMatchPlayers);
    switchToScreen(R.id.screen_wait);
    keepScreenOn();
    resetGameVars();

    Log.d(TAG, "Room created, waiting for it to be ready...");
  }

  // Handle the result of the invitation inbox UI, where the player can pick
  // an invitation
  // to accept. We react by accepting the selected invitation, if any.
  private void handleInvitationInboxResult(int response, Intent data) {
    if (response != Activity.RESULT_OK) {
      Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
      switchToMainScreen();
      return;
    }

    Log.d(TAG, "Invitation inbox UI succeeded.");
    Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

    // accept invitation
    acceptInviteToRoom(inv.getInvitationId());
  }

  // Accept the given invitation.
  void acceptInviteToRoom(String invId) {
    // accept the invitation
    keepScreenOn();
    resetGameVars();
    connectionHandler.acceptInviteToRoom(invId);
  }

  // Activity is going to the background. We have to leave the current room.
  @Override
  public void onStop() {
    Log.d(TAG, "**** got onStop");

    // if we're in a room, leave it.
    leaveRoom();

    // stop trying to keep the screen on
    stopKeepingScreenOn();

    if (connectionHandler.isConnected()) {
      switchToScreen(R.id.screen_sign_in);
    } else {
      switchToScreen(R.id.screen_wait);
    }
    super.onStop();
  }

  // Activity just got to the foreground. We switch to the wait screen because
  // we will now
  // go through the sign-in flow (remember that, yes, every time the Activity
  // comes back to the
  // foreground we go through the sign-in flow -- but if the user is already
  // authenticated,
  // this flow simply succeeds and is imperceptible).
  @Override
  public void onStart() {
    switchToScreen(R.id.screen_wait);
    if (connectionHandler.isConnected()) {
      Log.w(TAG, "GameHelper: client was already connected on onStart()");
    } else {
      Log.d(TAG, "Connecting client.");
      connectionHandler.connect();
    }
    super.onStart();
  }

  // Handle back key to make sure we cleanly leave a game if we are in the
  // middle of one
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent e) {
    if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
      leaveRoom();
      return true;
    }
    return super.onKeyDown(keyCode, e);
  }

  // Leave the room.
  void leaveRoom() {
    Log.d(TAG, "Leaving room.");
    mSecondsLeft = 0;
    stopKeepingScreenOn();
    if (connectionHandler.leaveRoom()) {
      switchToScreen(R.id.screen_wait);
    } else {
      switchToMainScreen();
    }
  }

  // Show the waiting room UI to track the progress of other players as they
  // enter the room and get connected.
  void showWaitingRoom(Room room) {
    // show waiting room UI
    startActivityForResult(connectionHandler.getWaitingRoomIntent(room), RC_WAITING_ROOM);
  }

  @Override
  public void onInvitationRemoved(String invitationId) {
    switchToScreen(mCurScreen); // This will hide the invitation popup
  }

  /*
   * CALLBACKS SECTION. This section shows how we implement the several games
   * API callbacks.
   */

  // Show error message about game being cancelled and return to main screen.
  @Override
  public void showGameError() {
    BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem));
    switchToMainScreen();
  }

  @Override
  public void updateRoom(Room room) {
    if (room != null) {
      mParticipants = room.getParticipants();
    }
    if (mParticipants != null) {
      updatePeerScoresDisplay();
    }
  }

  /*
   * GAME LOGIC SECTION. Methods that implement the game's rules.
   */

  // Current state of the game:
  int mSecondsLeft = -1; // how long until the game ends (seconds)
  final static int GAME_DURATION = 20; // game duration, seconds.
  int mScore = 0; // user's current score

  private float currentX;

  private float currentY;

  private float currentZ;

  // Reset game variables in preparation for a new game.
  void resetGameVars() {
    mSecondsLeft = GAME_DURATION;
    mScore = 0;
    mParticipantScore.clear();
    mFinishedParticipants.clear();
    currentGame = new Game(participantsToIds(mParticipants), mMyId);
  }

  private List<String> participantsToIds(List<Participant> participants) {
    List<String> result = new ArrayList<String>();
    for (Participant participant : mParticipants) {
      result.add(participant.getParticipantId());
    }
    return result;
  }

  // Start the gameplay phase of the game.
  void startGame(boolean multiplayer) {
    mMultiplayer = multiplayer;
    updateScoreDisplay();
    connectionHandler.broadcastScore(false);
    switchToScreen(R.id.screen_game);

    findViewById(R.id.button_head).setVisibility(View.VISIBLE);
    findViewById(R.id.button_left_hand).setVisibility(View.VISIBLE);
    findViewById(R.id.button_right_hand).setVisibility(View.VISIBLE);
    findViewById(R.id.button_left_foot).setVisibility(View.VISIBLE);
    findViewById(R.id.button_right_foot).setVisibility(View.VISIBLE);

    // run the gameTick() method every second to update the game.
    final Handler h = new Handler();
    h.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (mSecondsLeft <= 0) {
          return;
        }
        gameTick();
        h.postDelayed(this, 1000);
      }
    }, 1000);
  }

  // Game tick -- update countdown, check if game ended.
  void gameTick() {
    if (mSecondsLeft > 0) {
      --mSecondsLeft;
    }

    // update countdown
    ((TextView) findViewById(R.id.countdown)).setText("0:" + (mSecondsLeft < 10 ? "0" : "")
        + String.valueOf(mSecondsLeft));

    if (mSecondsLeft <= 0) {
      // finish game
      findViewById(R.id.button_head).setEnabled(false);
      findViewById(R.id.button_left_hand).setEnabled(false);
      findViewById(R.id.button_right_hand).setEnabled(false);
      findViewById(R.id.button_left_foot).setEnabled(false);
      findViewById(R.id.button_right_foot).setEnabled(false);

      connectionHandler.broadcastScore(true);
    }
  }

  // indicates the player scored one point
  void scoreOnePoint() {
    if (mSecondsLeft <= 0) {
      return; // too late!
    }
    ++mScore;
    updateScoreDisplay();
    updatePeerScoresDisplay();

    // broadcast our new score to our peers
    connectionHandler.broadcastScore(false);
  }

  void moveInDirection(float x, float y, float z) {
    if (mSecondsLeft <= 0) {
      return; // too late!
    }
    currentX = x;
    currentY = y;
    currentZ = z;
    updateScoreDisplay();
    updatePeerScoresDisplay();

    // broadcast our new score to our peers
    connectionHandler.broadcastScore(false);
  }

  /*
   * COMMUNICATIONS SECTION. Methods that implement the game's network protocol.
   */

  // Score of other participants. We update this as we receive their scores
  // from the network.
  Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();

  // Participants who sent us their final score.
  Set<String> mFinishedParticipants = new HashSet<String>();

  // Called when we receive a real-time message from the network.
  // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
  // indicating
  // whether it's a final or interim score. The second byte is the score.
  // There is also the
  // 'S' message, which indicates that the game should start.
  /*
   * UI SECTION. Methods that implement the game's UI.
   */

  // This array lists everything that's clickable, so we can install click
  // event handlers.
  final static int[] CLICKABLES = { R.id.button_accept_popup_invitation,
      R.id.button_invite_players, R.id.button_quick_game, R.id.button_see_invitations,
      R.id.button_sign_in, R.id.button_sign_out, R.id.button_head, R.id.button_left_hand,
      R.id.button_right_hand, R.id.button_left_foot, R.id.button_right_foot,
      R.id.button_single_player, R.id.button_single_player_2 };

  // This array lists all the individual screens our game has.
  final static int[] SCREENS = { R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
      R.id.screen_wait };
  int mCurScreen = -1;

  void switchToScreen(int screenId) {
    // make the requested screen visible; hide all others.
    for (int id : SCREENS) {
      findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
    }
    mCurScreen = screenId;

    // should we show the invitation popup?
    boolean showInvPopup;
    if (mIncomingInvitationId == null) {
      // no invitation, so no popup
      showInvPopup = false;
    } else if (mMultiplayer) {
      // if in multiplayer, only show invitation on main screen
      showInvPopup = (mCurScreen == R.id.screen_main);
    } else {
      // single-player: show on main screen and gameplay screen
      showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
    }
    findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
  }

  @Override
  public void switchToMainScreen() {
    if (connectionHandler.isConnected()) {
      switchToScreen(R.id.screen_main);
    } else {
      switchToScreen(R.id.screen_sign_in);
    }
  }

  // updates the label that shows my score
  void updateScoreDisplay() {
    ((TextView) findViewById(R.id.my_score)).setText(formatScore(currentX, currentY, currentZ));
  }

  // formats a score as a three-digit number
  String formatScore(float x, float y, float z) {
    return String.format("X:%f Y:%f Z:%f", x * 180 / Math.PI, y * 180 / Math.PI, z * 180 / Math.PI);
  }

  // formats a score as a three-digit number
  String formatScore(int i) {
    if (i < 0) {
      i = 0;
    }
    String s = String.valueOf(i);
    return s.length() == 1 ? "00" + s : s.length() == 2 ? "0" + s : s;
  }

  // updates the screen with the scores from our peers
  void updatePeerScoresDisplay() {
    ((TextView) findViewById(R.id.score0)).setText(formatScore(mScore) + " - Me");
    int[] arr = { R.id.score1, R.id.score2, R.id.score3 };
    int i = 0;

    if (connectionHandler.getRoomId() != null) {
      for (Participant p : mParticipants) {
        String pid = p.getParticipantId();
        if (pid.equals(mMyId)) {
          continue;
        }
        if (p.getStatus() != Participant.STATUS_JOINED) {
          continue;
        }
        int score = mParticipantScore.containsKey(pid) ? mParticipantScore.get(pid) : 0;
        ((TextView) findViewById(arr[i])).setText(formatScore(score) + " - " + p.getDisplayName());
        ++i;
      }
    }

    for (; i < arr.length; ++i) {
      ((TextView) findViewById(arr[i])).setText("");
    }
  }

  /*
   * MISC SECTION. Miscellaneous methods.
   */

  // Sets the flag to keep this screen on. It's recommended to do that during
  // the
  // handshake when setting up a game, because if the screen turns off, the
  // game will be
  // cancelled.
  void keepScreenOn() {
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  // Clears the flag that keeps the screen on.
  void stopKeepingScreenOn() {
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mSensorManager.unregisterListener(this);
  }

  @Override
  public void onKeepScreenOn() {
    keepScreenOn();
  }

  @Override
  public void onStopKeepingScreenOn() {
    stopKeepingScreenOn();
  }

  @Override
  public void onInvitation(Invitation invitation) {
    ((TextView) findViewById(R.id.incoming_invitation_text)).setText(invitation.getInviter()
        .getDisplayName() + " " + getString(R.string.is_inviting_you));

    switchToScreen(mCurScreen); // This will show the invitation popup
  }

  @Override
  public void onInvitationRemoved() {
    switchToScreen(mCurScreen);
  }

  @Override
  public boolean onConnectionFailed(GoogleApiClient googleApiClient,
      ConnectionResult connectionResult) {
    return BaseGameUtils.resolveConnectionFailure(this, googleApiClient, connectionResult,
        RC_SIGN_IN, getString(R.string.signin_other_error));
  }

  @Override
  public void onSwitchToWaitScreen() {
    // TODO Auto-generated method stub
  }

  @Override
  public void onShowWaitingRoom(Room room) {
    Intent i = connectionHandler.getWaitingRoomIntent(room);
    // show waiting room UI
    startActivityForResult(i, RC_WAITING_ROOM);
  }

  @Override
  public void onPeerScoresDislay() {
    // TODO Auto-generated method stub

  }

  @Override
  public void switchToSignInScreen() {
    switchToScreen(R.id.screen_sign_in);
  }

  @Override
  public void onAccuracyChanged(Sensor arg0, int arg1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onSensorChanged(SensorEvent arg0) {
    // TODO Auto-generated method stub

  }
}

// @Override
// protected void onCreate(Bundle savedInstanceState) {
// super.onCreate(savedInstanceState);
// AndroidApplicationConfiguration config = new
// AndroidApplicationConfiguration();
// initialize(new MyGdxGame(), config);
// }
// }
