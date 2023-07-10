package s0583823.util;

import lenz.htw.ai4g.ai.Info;
import s0583823.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FishTracker{
    private Info info;
    public List<FishInfo> fishInfos;
    public Map<Integer, Line2D> fishPaths;
    private int fishCount;
    public boolean isDone;

    public FishTracker(Info info){
        this.info = info;
        fishInfos = new LinkedList<>();
        fishPaths = new HashMap<>();
        fishCount = info.getScene().getFish().length;
        isDone = false;

        for (int i = 0; i < fishCount; i++) {
            fishInfos.add(new FishInfo(i));
        }
    }

    public void update() {
        int check = 0;
        for (FishInfo info: fishInfos) {
            if(info.notDone) {
                info.track();
                continue;
            }else {
                info.trackDirection();
            }
            if(fishPaths.containsKey(info.fishIndex)) {
                fishPaths.put(info.fishIndex, new Line2D.Float(info.maxLeft, (float) info.lastPos.getY(), info.maxRight, (float) info.lastPos.getY()));
                continue;
            }
            check++;
        }
        if(check == fishCount) isDone = true;
    }


    public float fishPosOnPlayerArrival(int arriveTime, int fishIndex){
        if (fishInfos.get(fishIndex).isSwimmingRight){
            float pos = (float) info.getScene().getFish()[fishIndex].getX() + arriveTime;
            return pos < fishInfos.get(fishIndex).maxRight ? pos : fishInfos.get(fishIndex).maxRight - (pos - fishInfos.get(fishIndex).maxRight);
        }

        float pos = (float) info.getScene().getFish()[fishIndex].getX() - arriveTime;
        return pos > fishInfos.get(fishIndex).maxLeft ? pos : fishInfos.get(fishIndex).maxLeft + (fishInfos.get(fishIndex).maxLeft - pos);
    }



    public class FishInfo{

        int fishIndex;
        public Point2D lastPos;
        public float maxLeft;
        public float maxRight;
        public boolean notDone;

        public FishInfo(int fishIndex){
            this.fishIndex = fishIndex;
            notDone = true;
        }

        boolean hasArrivedLeft = false;
        boolean hasArrivedRight = false;
        boolean isSwimmingRight;
        boolean lastState;
        int counter = 0;
        private void track() {

            if(counter <  1) {
                counter++;
                lastPos = info.getScene().getFish()[fishIndex].getLocation();
                return;
            }
            if(counter == 1) {
                counter++;
                isSwimmingRight = info.getScene().getFish()[fishIndex].getX() > lastPos.getX();
            }

            if(isSwimmingRight){
                if(lastPos.getX() > info.getScene().getFish()[fishIndex].getX()){
                    maxRight = (float) lastPos.getX();
                    hasArrivedRight = true;
                    isSwimmingRight = !isSwimmingRight;
//                    System.out.println("Change Direction");
                }
            }else {
                if(lastPos.getX() < info.getScene().getFish()[fishIndex].getX()){
                    maxLeft = (float) lastPos.getX();
                    hasArrivedLeft = true;
                    isSwimmingRight = !isSwimmingRight;
//                    System.out.println("Change Direction");
                }
            }

            if(hasArrivedRight && hasArrivedLeft) notDone = false;
            lastPos = info.getScene().getFish()[fishIndex].getLocation();
            //lastState = isSwimmingRight;
        }

        private void trackDirection(){
            if(isSwimmingRight){
                if(maxRight == info.getScene().getFish()[fishIndex].getX()){
                    isSwimmingRight = !isSwimmingRight;
                }
            }else {
                if(maxLeft ==  info.getScene().getFish()[fishIndex].getX()){
                    isSwimmingRight = !isSwimmingRight;
                }
            }
        }
    }
}