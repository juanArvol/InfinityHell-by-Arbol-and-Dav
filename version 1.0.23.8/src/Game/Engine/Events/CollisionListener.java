package Game.Engine.Events;

import Game.Engine.GameObjects;

public interface CollisionListener {

    default void onCollisionEnter(GameObjects other) {}
    default void onCollisionStay(GameObjects other) {}
    default void onCollisionExit(GameObjects other) {}

    default void onTriggerEnter(GameObjects other) {}
    default void onTriggerStay(GameObjects other) {}
    default void onTriggerExit(GameObjects other) {}
}