package s0583823;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DivingAction;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.ai.PlayerAction;
import s0583823.util.VectorF;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class DiveAi extends AI {
    private List<Point2D> pearls;
    AiState aiState;
    private int currentScore;
    Graph graph;


    //TODO:
    // Liste aus punkten die eine direkte Verbindung zur oberfläche haben.
    // Testlevel: mUe0uSHXvt ckttVQiGfc
    // Perlen in Bereiche einteilen -> z.B. oben-links, oben-rechts, unten-links, unten-rechts


    //Level nicht generierbar

    public DiveAi(Info info) {
        super(info);
        enlistForTournament(583823);
        pearls = new LinkedList(Arrays.asList(info.getScene().getPearl()));
        aiState = new AiState();
        graph = new Graph(info.getScene().getObstacles(), info.getScene().getWidth(), info.getScene().getHeight());

        long time = System.currentTimeMillis();
        graph.calculateReflexCorner();
        System.out.println(graph.reflexCorners.size());
        System.out.println(System.currentTimeMillis() - time + " milliseconds");
        sortLeftToRight(pearls);
        //sortNearest();
        graph.addStartEnd(new Point2D.Float(info.getX(), info.getY()), pearls.get(0));
        path = nodeToPoint2D(graph.findPathAStar());
        currentScore = 0;
        //target = path.peek();
    }

    private void sortNearest(List<Point2D> list) {
        list.sort((p1, p2) -> {
//            double distanceP1 = Math.hypot(p1.getX() - info.getX(), p1.getY() - info.getY());
//            double distanceP2 = Math.hypot(p2.getX() - info.getX(), p2.getY() - info.getY());
            Point2D aiPos = new Point2D.Float(info.getX(), info.getY());

            return Double.compare(p1.distance(aiPos), p2.distance(aiPos));
        });
    }

    private void sortLeftToRight(List<Point2D> list){
        list.sort((p1, p2) -> Double.compare(p1.getX(), p2.getX()));
    }



    @Override
    public String getName() {
        return "Suçuk";
    }
    @Override
    public Color getPrimaryColor() {
        return Color.BLUE;
    }
    @Override
    public Color getSecondaryColor() {
        return Color.orange;
    }


    Stack<Point2D> path;
    @Override
    public PlayerAction update() {

        float angularAcceleration = avoidCollision(aiState.getNextAction());
        float power = info.getMaxAcceleration();

        for (Point2D pearl: pearls) {
            if(currentScore < info.getScore() && info.getX() >= pearl.getX() - 8 && info.getX() <= pearl.getX() + 8 && info.getY() >= pearl.getY() - 8 && info.getY() <= pearl.getY() + 8){
                System.out.println("Pearl skipped");
                pearls.remove(pearl);
                break;
            }
        }

        return new DivingAction(power, angularAcceleration/5);
    }

    public void sortPearls(){
        sortNearest(pearls);
    }


    public VectorF seek(Point2D target){
        Point2D pos = new Point2D.Float(info.getX(), info.getY());
        return new VectorF(pos, target);
    }

    private float collisionRotate;
    public float avoidCollision(Point2D target){
        Point2D currentPoint = new Point2D.Float(info.getX(), info.getY());
        VectorF direction = new VectorF((float) (5 * Math.cos(info.getOrientation())), (float) (-5 * Math.sin(info.getOrientation()))).normalize();
        VectorF targetVec = new VectorF(currentPoint, target);

        boolean isColliding = false;
        boolean isTargetVecColliding = false;

        for (int i = 10; i >= 0; i--) {
            VectorF collisionRay = direction.normalize().multiplyScalar(i);
            VectorF targetCollisionRay = targetVec.normalize().multiplyScalar(.1f + i);
            Point2D targetCollisionCheck[] = new Point2D[]{targetCollisionRay.rotate2D(2).addToPoint(currentPoint), targetCollisionRay.rotate2D(-2).addToPoint(currentPoint)};
            Point2D.Float collisionCheck[] = new Point2D.Float[7];
            for (int k = -2; k <= 2; k++) {
                collisionCheck[k + 3] = collisionRay.rotate2D(30 * k).addToPoint(currentPoint);
            }
            collisionCheck[0] = new VectorF(-collisionRay.y, collisionRay.x).normalize().addToPoint(currentPoint);
            collisionCheck[6] = new VectorF(collisionRay.y, -collisionRay.x).normalize().addToPoint(currentPoint);

            for (Path2D path : info.getScene().getObstacles()) {
                if (path.contains(targetCollisionCheck[0]) && path.contains(targetCollisionCheck[1]) && targetCollisionRay.magnitude() < target.distance(currentPoint)) {
                    isTargetVecColliding = true;
                }

                if (path.contains(collisionCheck[2]) && path.contains(collisionCheck[3]) && path.contains(collisionCheck[4])) {
                    collisionRotate = align(seek(target));
                    isColliding = true;
                    continue;
                }
                if (path.contains(collisionCheck[2]) && path.contains(collisionCheck[3])) {
                    collisionRotate += 0.1;
                    isColliding = true;
                    continue;
                }
                if (path.contains(collisionCheck[3]) && path.contains(collisionCheck[4])) {
                    collisionRotate -= 0.1;
                    isColliding = true;
                    continue;
                }
                if(!isTargetVecColliding) {
                    if (path.contains(collisionCheck[0]) && !path.contains(collisionCheck[3])) {
                        collisionRotate += 0.005;
                        isColliding = true;
                    }
                    if (!path.contains(collisionCheck[3]) && path.contains(collisionCheck[6])) {
                        collisionRotate -= 0.005;
                        isColliding = true;
                    }
                }
            }
        }
        if(!isTargetVecColliding && !isColliding){
            collisionRotate -= Math.signum(collisionRotate) * 0.1;
            if(collisionRotate <= 0.05 || collisionRotate >= -0.05) collisionRotate = 0;
            return align(seek(target));
        }

        return collisionRotate;
    }

    public float align(VectorF targetDirection){
        float targetOrientation = - (float)Math.atan2(targetDirection.y, targetDirection.x);
        float angle = targetOrientation - info.getOrientation();
        if(Math.abs(angle) < 0.001f) return 0;
        if(Math.abs(angle) < 0.2f){
            return ((angle * info.getMaxAbsoluteAngularVelocity()) / 0.2f) - info.getAngularVelocity();
        }
        return Math.signum(angle) * info.getMaxAbsoluteAngularVelocity() - info.getAngularVelocity();
    }

    //region Old Code
    //IntelliJ Region
//    double breakRadius = 0;
//    public float arrive(){
//        double distance = Math.sqrt(Math.pow(target.peek().getX() - info.getX(), 2) + Math.pow(target.peek().getY() - info.getY(), 2));
//        if(distance <= 0.2f) return 0;
//        if(breakRadius < distance) breakRadius = Math.pow(info.getVelocity(),2) / (2 * info.getMaxAcceleration());
//        if(distance <= breakRadius){
//            return -info.getMaxAcceleration();
//        }
//        return info.getMaxVelocity();
//    }
    //endregion



    public class AiState {
        public Action swimAction;

        public AiState(){
            this.swimAction = Action.SwimToSurface;
        }

        public Point2D getNextAction(){

            switch (swimAction){
                case SwimToPearl:
                    if(!hasArrivedAtTarget()) break;

                    Stack<Graph.Node> tmp = new Stack<>();
                    if(nextTargetInReach(tmp)){
                        path = nodeToPoint2D(tmp);
                        break;
                    }
                    switchState();
                    break;
                case SwimToSurface:
                    if(!hasArrivedAtSurface()) break;

                    switchState();
                    break;
            }
            return path.peek();
        }


        private void switchState() {
            if(this.swimAction.equals(Action.SwimToPearl)) {
                this.swimAction = Action.SwimToSurface;
                path = nodeToPoint2D(graph.getPathToAir(new Point2D.Float(info.getX(), info.getY())));
            }
            else {
                this.swimAction = Action.SwimToPearl;
                Point2D target = getNextTarget();
                Stack<Graph.Node> tmp = new Stack<>();
                path.clear();
                Point2D tmpTarget = (Point2D) target.clone();
                if(!nextTargetInReach(tmp)) {
                    tmpTarget = new Point2D.Float((float) target.getX(), 40);
                    graph.updateStartEnd(tmpTarget, target);
                    path = nodeToPoint2D(graph.findPathAStar());
                }
                graph.updateStartEnd(new Point2D.Float(info.getX(), info.getY()), tmpTarget);
                path.addAll(nodeToPoint2D(graph.findPathAStar()));
            }
        }

        private boolean hasArrivedAtTarget() {
            if(info.getX() >= path.peek().getX() - 10 && info.getX() <= path.peek().getX() + 10 && info.getY() >= path.peek().getY() - 10 && info.getY() <= path.peek().getY() + 10){
                path.pop();
                Point2D target = pearls.isEmpty() ? lastTarget : pearls.get(0);
                if(path.empty() && info.getX() >= target.getX() - 10 && info.getX() <= target.getX() + 10 && info.getY() >= target.getY() - 10 && info.getY() <= target.getY() + 10) {
                    currentScore++;
                    if(!pearls.isEmpty()) pearls.remove(0);
                    System.out.println("Arrived Pearl");
                    return true;
                }
            }
            return false;
        }

        //TODO: check if last 2 pearls are in reach
        private boolean nextTargetInReach(Stack<Graph.Node> tmp) {
            Point2D target = getNextTarget();
            graph.updateStartEnd(new Point2D.Float(info.getX(), info.getY()), target);
            tmp.addAll(graph.findPathAStar());
            if(currentScore < 9) {
                if (tmp.get(0).distanceToPrev + (target.getY() - 40) < info.getAir()) return true;
            }else {
                if (tmp.get(0).distanceToPrev < info.getAir()) return true;
            }
            return false;
        }

        private boolean hasArrivedAtSurface() {
            if(info.getX() >= path.peek().getX() - 9.5 && info.getX() <= path.peek().getX() + 9.5 && info.getY() >= path.peek().getY() - 9.5 && info.getY() <= path.peek().getY() + 9.5) {
                path.pop();
            }
            if(info.getAir() == info.getMaxAir()) return true;
            return false;
        }

        private Point2D lastTarget;
        private Point2D getNextTarget(){
            if(pearls.isEmpty()) return lastTarget;
            sortPearls();
            lastTarget = pearls.get(0);
            return pearls.get(0);
        }


        private enum Action{
            SwimToPearl, SwimToSurface
        }
    }

    //TODO:
//    public void hasEnoughAir(float distancePearl, double pearlPosY){
//        System.out.println("Distance: " + (distancePearl + pearlPosY) + "\tAir: " + info.getAir());
//        if((distancePearl + pearlPosY) > info.getAir() * 1.05){
//
//        }
//    }
//
//    public boolean airReplenished(int air){
//        if(air == info.getMaxAir()){
//
//        }
//
//    }


    private Stack<Point2D> nodeToPoint2D(Stack<Graph.Node> s){
        Stack<Point2D> out = new Stack<>();
        for (int i = 0; i < s.size(); i++) {
            out.push(graph.reflexCorners.get(s.get(i).index));
        }
        return out;
    }

    @Override
    public void drawDebugStuff(Graphics2D gfx) {
        super.drawDebugStuff(gfx);

        //obstacles
//        gfx.setColor(Color.BLUE);
//        gfx.draw(info.getScene().getObstacles()[0]);

        //Collision Checker
//        gfx.setColor(Color.BLACK);
//        VectorF direction = new VectorF((float) (5 * Math.cos(info.getOrientation())), (float) (-5 * Math.sin(info.getOrientation()))).normalize();
//        Point2D currentPoint = new Point2D.Float(info.getX(), info.getY());
//        for (int i = 10; i >= 0; i--) {
//            VectorF collisionRay = direction.normalize().multiplyScalar(i);
//            Point2D.Float collisionCheck[] = new Point2D.Float[7];
//            for (int k = -3; k <= 3; k++) {
//                collisionCheck[k + 3] = collisionRay.rotate2D(30 * k).addToPoint(currentPoint);
//                gfx.fillOval((int) collisionCheck[k + 3].getX(), (int) collisionCheck[k + 3].getY(), 5,5);
//            }
//        }


//        if (test != null) gfx.drawLine((int) currentPoint.getX(), (int) currentPoint.getY(), (int) test.addToPoint(currentPoint).getX(), (int) test.addToPoint(currentPoint).getY());

        //ReflexCorners
        gfx.setColor(Color.RED);
        for (int i = 0; i < this.graph.reflexCorners.size() - 2; i++) {
            gfx.fillOval((int) this.graph.reflexCorners.get(i).getX() - 5, (int) this.graph.reflexCorners.get(i).getY() - 5, 10,10);
        }

        //Reflex under Air
        gfx.setColor(Color.ORANGE);
        for (int i = 0; i < this.graph.reflexToAir.size(); i++) {
            gfx.fillOval((int)this.graph.reflexToAir.get(i).getX() - 3, (int)this.graph.reflexToAir.get(i).getY() - 3, 6,6);
        }

        // Draw Star/End
        gfx.setColor(Color.GREEN);
        gfx.fillOval((int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-2).getX()-5), (int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-2).getY()-5), 10 ,10);
        gfx.fillOval((int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-1).getX()-5), (int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-1).getY()-5), 10 ,10);

        //Draw Path
        gfx.setColor(Color.BLACK);
        List<Point2D> l = path.stream().toList();
        Point2D p = pearls.get(0);
        for (int i = 0; i < path.size(); i++) {
            gfx.drawLine((int) p.getX(), (int) p.getY(), (int) l.get(i).getX(), (int) l.get(i).getY());
            p = l.get(i);
        }
        gfx.drawLine((int) info.getX(), (int) info.getY(), (int) l.get(l.size()-1).getX(), (int) l.get(l.size()-1).getY());

        // Draw Graph
//        gfx.setColor(Color.GREEN);
//        for (int i = 0; i < graph.graph.length-2; i++) {
//            for (int j = i; j < graph.graph.length-2; j++) {
//                if(graph.graph[i][j] < Float.POSITIVE_INFINITY){
//                    Point2D p1 = new Point2D.Float((float) graph.reflexCorners.get(i).getX(), (float) graph.reflexCorners.get(i).getY());
//                    Point2D p2 = new Point2D.Float((float) graph.reflexCorners.get(j).getX(), (float) graph.reflexCorners.get(j).getY());
//                    gfx.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
//                }
//            }
//        }

        //Draw Obstacles
//        for (List<Point2D> l: graph.obstaclePoints) {
//            for (int i = 0; i < l.size(); i++) {
//                if(i<l.size()/3) gfx.setColor(Color.RED);
//                if(i<l.size() * 2/3 && i >= l.size()/3) gfx.setColor(Color.BLUE);
//                if(i>= l.size() * 2/3) gfx.setColor(Color.GREEN);
//                Point2D p = l.get(i);
//                gfx.fillOval((int) (p.getX()-5), (int) (p.getY()-5),10,10);
//            }
//        }
    }
}