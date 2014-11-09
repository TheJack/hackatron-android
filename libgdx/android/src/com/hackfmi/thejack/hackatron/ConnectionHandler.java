package com.hackfmi.thejack.hackatron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.plus.Plus;

public class ConnectionHandler implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, RealTimeMessageReceivedListener,
    RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {
  /*
   * API INTEGRATION SECTION. This section contains the code that integrates the
   * game with the Google Play game services API.
   */

  final static String TAG = "ButtonClicker2000";

  // Client used to interact with Google APIs.
  private GoogleApiClient mGoogleApiClient;

  // Are we currently resolving a connection failure?
  boolean mResolvingConnectionFailure = false;

  // Has the user clicked the sign-in button?
  boolean mSignInClicked = false;

  // Set to true to automatically start the sign in flow when the Activity
  // starts.
  // Set to false to require the user to click the button in order to sign in.
  private boolean mAutoStartSignInFlow = true;

  // Room ID where the currently active game is taking place; null if we're
  // not playing.
  String mRoomId = null;

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

  // Request code used to invoke sign in user interactions.
  private static final int RC_SIGN_IN = 9001;

  // Game
  private Game currentGame;

  private Context context;

  private ChangeListener listener;

  public static final ConnectionHandler instance = new ConnectionHandler();

  public static interface ChangeListener {
    void onInvitation(Invitation invitation);

    void onInvitationRemoved();

    boolean onConnectionFailed(GoogleApiClient googleApiClient, ConnectionResult connectionResult);

    void onSwitchToWaitScreen();

    void onShowWaitingRoom(Room room);

    void onPeerScoresDislay();

    void switchToSignInScreen();

    void switchToMainScreen();

    void showGameError();

    void updateRoom(Room room);

    void onInvitationRemoved(String invitationId);
  }

  public ConnectionHandler() {
    mParticipants = new ArrayList<Participant>();
  }

  void registerNewContext(Context context) {
    this.context = context;
    // Create the Google Api Client with access to Plus and Games
    mGoogleApiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this).addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
        .addApi(Games.API).addScope(Games.SCOPE_GAMES).build();
  }

  public static ConnectionHandler registerContext(Context context) {
    instance.registerNewContext(context);
    return instance;
  }

  public void startQuickGame() {
    // quick-start a game with 1 randomly selected opponent
    final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
    Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS, MAX_OPPONENTS, 0);
    RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
    rtmConfigBuilder.setMessageReceivedListener(this);
    rtmConfigBuilder.setRoomStatusUpdateListener(this);
    rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
    Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
  }

  // Accept the given invitation.
  public void acceptInviteToRoom(String invId) {
    // accept the invitation
    Log.d(TAG, "Accepting invitation: " + invId);
    RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
    roomConfigBuilder.setInvitationIdToAccept(invId).setMessageReceivedListener(this)
        .setRoomStatusUpdateListener(this);
    Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
  }

  // Leave the room.
  public boolean leaveRoom() {
    if (mRoomId != null) {
      Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
      mRoomId = null;
      return true;
    } else {
      return false;
    }
  }

  // Called when we get an invitation to play a game. We react by showing that
  // to the user.
  @Override
  public void onInvitationReceived(Invitation invitation) {
    // We got an invitation to play a game! So, store it in
    // mIncomingInvitationId
    // and show the popup on the screen.
    mIncomingInvitationId = invitation.getInvitationId();
    notifyInvitation(invitation);
  }

  @Override
  public void onConnected(Bundle connectionHint) {
    Log.d(TAG, "onConnected() called. Sign in successful!");

    Log.d(TAG, "Sign-in succeeded.");

    // register listener so we are notified if we receive an invitation to
    // play
    // while we are in the game
    Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

    if (connectionHint != null) {
      Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
      Invitation inv = connectionHint.getParcelable(Multiplayer.EXTRA_INVITATION);
      if (inv != null && inv.getInvitationId() != null) {
        // retrieve and cache the invitation ID
        Log.d(TAG, "onConnected: connection hint has a room invite!");
        acceptInviteToRoom(inv.getInvitationId());
        return;
      }
    }
    switchToMainScreen();
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
    mGoogleApiClient.connect();
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

    if (mResolvingConnectionFailure) {
      Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
      return;
    }

    if (mSignInClicked || mAutoStartSignInFlow) {
      mAutoStartSignInFlow = false;
      mSignInClicked = false;

      mResolvingConnectionFailure = notifyConnectionFailed(mGoogleApiClient, connectionResult);
    }

    switchToSignInScreen();
  }

  private void switchToSignInScreen() {
    if (listener != null) {
      listener.switchToSignInScreen();
    }
  }

  private boolean notifyConnectionFailed(GoogleApiClient googleApiClient,
      ConnectionResult connectionResult) {
    if (listener != null) {
      return listener.onConnectionFailed(googleApiClient, connectionResult);
    }
    return true;
  }

  // Called when we are connected to the room. We're not ready to play yet!
  // (maybe not everybody
  // is connected yet).
  @Override
  public void onConnectedToRoom(Room room) {
    Log.d(TAG, "onConnectedToRoom.");

    // get room ID, participants and my ID:
    mRoomId = room.getRoomId();
    mParticipants = room.getParticipants();
    mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

    // print out the list of participants (for debug purposes)
    Log.d(TAG, "Room ID: " + mRoomId);
    Log.d(TAG, "My ID " + mMyId);
    Log.d(TAG, "<< CONNECTED TO ROOM>>");
  }

  // Called when we've successfully left the room (this happens a result of
  // voluntarily leaving
  // via a call to leaveRoom(). If we get disconnected, we get
  // onDisconnectedFromRoom()).
  @Override
  public void onLeftRoom(int statusCode, String roomId) {
    // we have left the room; return to main screen.
    Log.d(TAG, "onLeftRoom, code " + statusCode);
    switchToMainScreen();
  }

  private void switchToMainScreen() {
    if (listener != null) {
      listener.switchToMainScreen();
    }
  }

  // Called when we get disconnected from the room. We return to the main
  // screen.
  @Override
  public void onDisconnectedFromRoom(Room room) {
    mRoomId = null;
    showGameError();
  }

  // Show error message about game being cancelled and return to main screen.
  void showGameError() {
    if (listener != null) {
      listener.showGameError();
    }
  }

  // Called when room has been created
  @Override
  public void onRoomCreated(int statusCode, Room room) {
    Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
    if (statusCode != GamesStatusCodes.STATUS_OK) {
      Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
      showGameError();
      return;
    }

    // show the waiting room UI
    showWaitingRoom(room);
  }

  private void showWaitingRoom(Room room) {
    if (listener != null) {
      listener.onShowWaitingRoom(room);
    }
  }

  // Called when room is fully connected.
  @Override
  public void onRoomConnected(int statusCode, Room room) {
    Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
    if (statusCode != GamesStatusCodes.STATUS_OK) {
      Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
      showGameError();
      return;
    }
    updateRoom(room);
  }

  @Override
  public void onJoinedRoom(int statusCode, Room room) {
    Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
    if (statusCode != GamesStatusCodes.STATUS_OK) {
      Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
      showGameError();
      return;
    }

    // show the waiting room UI
    showWaitingRoom(room);
  }

  public Intent getSelectOpponentsIntent() {
    return Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 3);
  }

  public Intent getWaitingRoomIntent(Room room) {
    final int MIN_PLAYERS = Integer.MAX_VALUE;
    return Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);
  }

  public Intent getInvitationInboxIntent() {
    return Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
  }

  // We treat most of the room update callbacks in the same way: we update our
  // list of
  // participants and update the display. In a real game we would also have to
  // check if that
  // change requires some action like removing the corresponding player avatar
  // from the screen,
  // etc.
  @Override
  public void onPeerDeclined(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onPeerInvitedToRoom(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onP2PDisconnected(String participant) {
  }

  @Override
  public void onP2PConnected(String participant) {
  }

  @Override
  public void onPeerJoined(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onPeerLeft(Room room, List<String> peersWhoLeft) {
    updateRoom(room);
  }

  @Override
  public void onRoomAutoMatching(Room room) {
    updateRoom(room);
  }

  @Override
  public void onRoomConnecting(Room room) {
    updateRoom(room);
  }

  @Override
  public void onPeersConnected(Room room, List<String> peers) {
    updateRoom(room);
  }

  @Override
  public void onPeersDisconnected(Room room, List<String> peers) {
    updateRoom(room);
  }

  void updateRoom(Room room) {
    if (listener != null) {
      listener.updateRoom(room);
    }
  }

  private void notifyPeerScoresDisplay() {
    if (listener != null) {
      listener.onPeerScoresDislay();
    }
  }

  // Current state of the game:
  int mSecondsLeft = -1; // how long until the game ends (seconds)
  final static int GAME_DURATION = 20; // game duration, seconds.
  int mScore = 0; // user's current score

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
  @Override
  public void onRealTimeMessageReceived(RealTimeMessage rtm) {
    byte[] buf = rtm.getMessageData();
    String sender = rtm.getSenderParticipantId();
    Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);

    if (buf[0] == 'F' || buf[0] == 'U') {
      // score update.
      int existingScore = mParticipantScore.containsKey(sender) ? mParticipantScore.get(sender) : 0;
      int thisScore = buf[1];
      if (thisScore > existingScore) {
        // this check is necessary because packets may arrive out of
        // order, so we
        // should only ever consider the highest score we received, as
        // we know in our
        // game there is no way to lose points. If there was a way to
        // lose points,
        // we'd have to add a "serial number" to the packet.
        mParticipantScore.put(sender, thisScore);
      }

      // update the scores on the screen
      notifyPeerScoresDisplay();

      // if it's a final score, mark this participant as having finished
      // the game
      if ((char) buf[0] == 'F') {
        mFinishedParticipants.add(rtm.getSenderParticipantId());
      }
    }
  }

  // Broadcast my score to everybody else.
  void broadcastScore(boolean finalScore) {
    if (!mMultiplayer) {
      return; // playing single-player mode
    }

    // First byte in message indicates whether it's a final score or not
    mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');

    // Second byte is the score.
    mMsgBuf[1] = (byte) mScore;

    // Send to every other participant.
    for (Participant p : mParticipants) {
      if (p.getParticipantId().equals(mMyId)) {
        continue;
      }
      if (p.getStatus() != Participant.STATUS_JOINED) {
        continue;
      }
      if (finalScore) {
        // final score notification must be sent via reliable message
        Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, mMsgBuf, mRoomId,
            p.getParticipantId());
      } else {
        // it's an interim score notification, so we can use unreliable
        Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, mMsgBuf, mRoomId,
            p.getParticipantId());
      }
    }
  }

  public void registerListener(ChangeListener listener) {
    this.listener = listener;
  }

  public void unregisterListener() {
    listener = null;
  }

  private void notifyInvitation(Invitation invitation) {
    if (listener != null) {
      listener.onInvitation(invitation);
    }
  }

  @Override
  public void onInvitationRemoved(String invitationId) {
    if (mIncomingInvitationId.equals(invitationId)) {
      mIncomingInvitationId = null;
      if (listener != null) {
        listener.onInvitationRemoved(invitationId);
      }
    }
  }

  public void connect() {
    mGoogleApiClient.connect();
  }

  public void createRoom(ArrayList<String> invitees, int minAutoMatchPlayers,
      int maxAutoMatchPlayers) {
    // get the automatch criteria
    Bundle autoMatchCriteria = null;
    if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
      autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers,
          maxAutoMatchPlayers, 0);
      Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
    }

    RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
    rtmConfigBuilder.addPlayersToInvite(invitees);
    rtmConfigBuilder.setMessageReceivedListener(this);
    rtmConfigBuilder.setRoomStatusUpdateListener(this);
    if (autoMatchCriteria != null) {
      rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
    }

    Games.RealTimeMultiplayer.create(mGoogleApiClient, rtmConfigBuilder.build());
  }

  public boolean isConnected() {
    return mGoogleApiClient != null && mGoogleApiClient.isConnected();
  }

  public String getRoomId() {
    return mRoomId;
  }

  public void disconnect() {
    Games.signOut(mGoogleApiClient);
    mGoogleApiClient.disconnect();
  }
}
