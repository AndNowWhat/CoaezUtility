package net.botwithus.tasks.sorceressgarden.models;

import net.botwithus.rs3.game.Coordinate;

/**
 * Enum representing the 8 cardinal and intercardinal directions for NPCs
 * Used for tracking guardian directions in Sorceress's Garden
 */
public class NPCDirection {
    public enum Direction {
        NORTH(157.5, 202.5),
        SOUTH(337.5, 22.5),
        EAST(67.5, 112.5),
        WEST(247.5, 292.5),
        NORTH_EAST(112.5, 157.5),
        NORTH_WEST(202.5, 247.5),
        SOUTH_EAST(22.5, 67.5),
        SOUTH_WEST(292.5, 337.5);

        private final double minAngle;
        private final double maxAngle;

        /**
         * Constructs a Direction with specified angle range.
         *
         * @param minAngle the minimum angle for the direction in degrees.
         * @param maxAngle the maximum angle for the direction in degrees.
         */
        Direction(double minAngle, double maxAngle) {
            this.minAngle = minAngle;
            this.maxAngle = maxAngle;
        }


        /**
         * Determines the facing direction based on the NPC's direction values (direction1, direction2).
         *
         * @param direction1 the first direction value from the NPC.
         * @param direction2 the second direction value from the NPC.
         * @return the Direction enum that matches the calculated angle, or null if no match is found.
         */
        public static Direction getFacingDirection(float direction1, float direction2) {
            // Convert direction1 to degrees (assuming it's in radians)
            double angle = Math.toDegrees(direction1);
            // Normalize to 0-360 range
            angle = (angle + 360) % 360;
            
            for (Direction direction : Direction.values()) {
                if (isInRange(direction, angle)) {
                    return direction;
                }
            }
            return null;
        }

        /**
         * Checks if a given angle falls within the specified range of a Direction.
         *
         * @param direction the Direction to check.
         * @param angle the angle to evaluate in degrees.
         * @return true if the angle is within the direction's range; false otherwise.
         */
        public static boolean isInRange(Direction direction, double angle) {
            if (direction == SOUTH) {
                return angle >= 337.5 || angle < 22.5;
            }
            return angle >= direction.minAngle && angle < direction.maxAngle;
        }

        /**
         * Calculates a target position by moving a certain distance in a specified direction from the current position.
         *
         * @param currentPosition the starting Coordinate.
         * @param direction the Direction to move towards.
         * @param distance the number of tiles to move in the specified direction.
         * @return the resulting Coordinate after applying the offset.
         */
        public static Coordinate getOffsetPosition(Coordinate currentPosition, Direction direction, int distance) {
            int xOffset = 0, yOffset = 0;

            switch (direction) {
                case NORTH:
                    yOffset = -distance;
                    break;
                case SOUTH:
                    yOffset = distance;
                    break;
                case EAST:
                    xOffset = -distance;
                    break;
                case WEST:
                    xOffset = distance;
                    break;
                case NORTH_EAST:
                    xOffset = -distance;
                    yOffset = -distance;
                    break;
                case NORTH_WEST:
                    xOffset = distance;
                    yOffset = -distance;
                    break;
                case SOUTH_EAST:
                    xOffset = -distance;
                    yOffset = distance;
                    break;
                case SOUTH_WEST:
                    xOffset = distance;
                    yOffset = distance;
                    break;
            }
            return currentPosition.derive(xOffset, yOffset, 0);
        }
    }
} 