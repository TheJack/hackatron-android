package com.hackfmi.thejack.hackatron;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
  private final String myId;
  private final Map<String, List<BodyPart>> controls;
  private final List<String> participantIds;

  public Game(List<String> participantIds, String myId) {
    this.participantIds = participantIds;
    this.myId = myId;
    controls = new HashMap<String, List<BodyPart>>();
  }

  public void start() {

  }

  public void playerRotate(float x, float y, float z, float tita) {

  }

  public void playerRotate(String participantId, float x, float y, float z, float tita) {

  }

  void use(BodyPart bodyPart) {
    switch (bodyPart) {
    case HEAD:
      break;
    case LEFT_HAND:
      break;
    case RIGHT_HAND:
      break;
    case LEFT_FOOT:
      break;
    case RIGHT_FOOT:
      break;
    }
  }

  enum BodyPart {
    HEAD, LEFT_HAND, RIGHT_HAND, LEFT_FOOT, RIGHT_FOOT;
  }
}
