package Game.Engine.Components.Collisions;

import java.awt.Rectangle;

public class SweptAABB {

    public static class Result {

        public double time;
        public int normalX;
        public int normalY;

        public Result(double time, int normalX, int normalY) {
            this.time = time;
            this.normalX = normalX;
            this.normalY = normalY;
        }

        public boolean hasCollision() {
            return time >= 0.0 && time < 1.0;
        }
    }

    public static Result calculate(
            Rectangle moving,
            Rectangle target,
            double velocityX,
            double velocityY
    ) {

        double xInvEntry, yInvEntry;
        double xInvExit, yInvExit;

        if (velocityX > 0.0) {
            xInvEntry = target.x - (moving.x + moving.width);
            xInvExit = (target.x + target.width) - moving.x;
        } else {
            xInvEntry = (target.x + target.width) - moving.x;
            xInvExit = target.x - (moving.x + moving.width);
        }

        if (velocityY > 0.0) {
            yInvEntry = target.y - (moving.y + moving.height);
            yInvExit = (target.y + target.height) - moving.y;
        } else {
            yInvEntry = (target.y + target.height) - moving.y;
            yInvExit = target.y - (moving.y + moving.height);
        }

        double xEntry, yEntry;
        double xExit, yExit;

        if (velocityX == 0.0) {
            xEntry = Double.NEGATIVE_INFINITY;
            xExit = Double.POSITIVE_INFINITY;
        } else {
            xEntry = xInvEntry / velocityX;
            xExit = xInvExit / velocityX;
        }

        if (velocityY == 0.0) {
            yEntry = Double.NEGATIVE_INFINITY;
            yExit = Double.POSITIVE_INFINITY;
        } else {
            yEntry = yInvEntry / velocityY;
            yExit = yInvExit / velocityY;
        }

        double entryTime = Math.max(xEntry, yEntry);
        double exitTime = Math.min(xExit, yExit);

        if (entryTime > exitTime || entryTime > 1.0) {
            return new Result(1.0, 0, 0);
        }

        entryTime = Math.max(entryTime, 0.0);

        int normalX = 0;
        int normalY = 0;

        if (xEntry > yEntry) {

            if (velocityX > 0.0)
                normalX = -1;
            else
                normalX = 1;

        } else {

            if (velocityY > 0.0)
                normalY = -1;
            else
                normalY = 1;
        }

        return new Result(entryTime, normalX, normalY);
    }
}