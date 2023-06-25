package s0583823.util;

import java.awt.geom.Point2D;

public class FishTracker {





    private class FishInfo implements Runnable{

        Point2D fishPos;
        Point2D startPos;
        double maxLeft;
        double maxRight;

        public FishInfo(Point2D fishPos){
            this.fishPos = fishPos;
            startPos = (Point2D) fishPos.clone();
            maxLeft = fishPos.getX();
            maxRight = fishPos.getY();
        }

        @Override
        public void run() {
            boolean hasArrivedLeft = false;
            boolean hasArrivedRight = false;
            Boolean isSwimmingRight = null;

            while (!hasArrivedLeft && !hasArrivedRight){

                maxRight = Math.max(maxRight, fishPos.getX());
                maxLeft = Math.min(maxLeft, fishPos.getX());

                if(isSwimmingRight == null) isSwimmingRight = maxRight > startPos.getX();

                if(isSwimmingRight){
                    hasArrivedRight = fishPos.getX() < maxRight;
                }else {
                    hasArrivedLeft = fishPos.getX() > maxLeft;
                }
            }
        }
    }
}
