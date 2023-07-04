package s0583823.util;

import lenz.htw.ai4g.ai.Info;
import s0583823.Graph;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FishTracker implements Runnable{
    private Info info;
    private Graph graph;

    public FishTracker(Info info, Graph graph){
        this.info = info;
        this.graph = graph;
    }

    @Override
    public void run() {
        int fishCount = info.getScene().getFish().length;
        ExecutorService service = Executors.newFixedThreadPool(fishCount);
        List< Future > tasks = new ArrayList<>();

        for (int i = 0; i < fishCount; i++) {
            tasks.add(service.submit(new FishInfo(i)));
        }

        for (Future future:tasks) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class FishInfo implements Runnable{

        int fishIndex;
        Point2D startPos;
        double maxLeft;
        double maxRight;

        public FishInfo(int fishIndex){
            this.fishIndex = fishIndex;
            startPos = info.getScene().getFish()[fishIndex];
            maxLeft = startPos.getX();
            maxRight = startPos.getX();
        }

        @Override
        public void run() {
            boolean hasArrivedLeft = false;
            boolean hasArrivedRight = false;
            Boolean isSwimmingRight = null;

            while (!hasArrivedLeft && !hasArrivedRight){

                maxRight = Math.max(maxRight, info.getScene().getFish()[fishIndex].getX());
                maxLeft = Math.min(maxLeft, info.getScene().getFish()[fishIndex].getX());

                if(isSwimmingRight == null) isSwimmingRight = maxRight > startPos.getX();

                if(isSwimmingRight){
                    hasArrivedRight = info.getScene().getFish()[fishIndex].getX() < maxRight;
                }else {
                    hasArrivedLeft = info.getScene().getFish()[fishIndex].getX() > maxLeft;
                }
            }

        }
    }
}