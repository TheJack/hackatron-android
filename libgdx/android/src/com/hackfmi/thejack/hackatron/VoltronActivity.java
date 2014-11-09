package com.hackfmi.thejack.hackatron;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.hackfmi.thejack.hackatron.ConnectionHandler.ChangeListener;
import com.mygdx.game.MyGdxGame;

public class VoltronActivity extends AndroidApplication implements ChangeListener,
    SensorEventListener {
  private ConnectionHandler connectionHandler;
  private SensorManager mSensorManager;
  private Sensor mSensor;
  private MyGdxGame voltron;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    connectionHandler = ConnectionHandler.registerContext(this);
    connectionHandler.registerListener(this);

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

    AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
    voltron = new MyGdxGame();
    initialize(voltron, config);
  }

  @Override
  public void onInvitation(Invitation invitation) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onInvitationRemoved() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean onConnectionFailed(GoogleApiClient googleApiClient,
      ConnectionResult connectionResult) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void onSwitchToWaitScreen() {
    // TODO Auto-generated method stub

  }

  @Override
  public void onShowWaitingRoom(Room room) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onPeerScoresDislay() {
    // TODO Auto-generated method stub

  }

  @Override
  public void switchToSignInScreen() {
    // TODO Auto-generated method stub

  }

  @Override
  public void switchToMainScreen() {
    // TODO Auto-generated method stub

  }

  @Override
  public void showGameError() {
    // TODO Auto-generated method stub
    System.out.println("SHOW ERROR");
  }

  @Override
  public void updateRoom(Room room) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onInvitationRemoved(String invitationId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // TODO Auto-generated method stub

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

  // Create a constant to convert nanoseconds to seconds.
  private static final float NS2S = 1.0f / 1000000000.0f;
  private static final float EPSILON = 1e-9f;
  private final float[] deltaRotationVector = new float[4];
  private float timestamp;

  @Override
  public void onSensorChanged(SensorEvent event) {
    // This timestep's delta rotation to be multiplied by the current rotation
    // after computing it from the gyro sample data.
    if (timestamp != 0) {
      final float dT = (event.timestamp - timestamp) * NS2S;
      // Axis of the rotation sample, not normalized yet.
      float axisX = event.values[0];
      float axisY = event.values[1];
      float axisZ = event.values[2];

      // Calculate the angular speed of the sample
      float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

      // Normalize the rotation vector if it's big enough to get the axis
      // (that is, EPSILON should represent your maximum allowable margin of
      // error)
      if (omegaMagnitude > EPSILON) {
        axisX /= omegaMagnitude;
        axisY /= omegaMagnitude;
        axisZ /= omegaMagnitude;
      }

      // Integrate around this axis with the angular speed by the timestep
      // in order to get a delta rotation from this sample over the timestep
      // We will convert this axis-angle representation of the delta rotation
      // into a quaternion before turning it into the rotation matrix.
      float thetaOverTwo = omegaMagnitude * dT / 2.0f;
      float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
      float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
      deltaRotationVector[0] = sinThetaOverTwo * axisX;
      deltaRotationVector[1] = sinThetaOverTwo * axisY;
      deltaRotationVector[2] = sinThetaOverTwo * axisZ;
      deltaRotationVector[3] = cosThetaOverTwo;
    }
    timestamp = event.timestamp;
    if (voltron != null) {
      voltron.touchDragged((int) (100 * event.values[0]), (int) (100 * event.values[1]), 1);
    }
    // User code should concatenate the delta rotation we computed with the
    // current rotation
    // in order to get the updated rotation.
    // rotationCurrent = rotationCurrent * deltaRotationMatrix;
  }
}
