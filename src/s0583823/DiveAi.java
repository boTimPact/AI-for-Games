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
import java.util.stream.Collectors;


public class DiveAi extends AI {
    private List<Point2D> pearls;
    private Queue<Point2D> target;
    private int currentScore;
    Graph graph;


    //TODO:
    // Liste aus punkten die eine direkte Verbindung zur oberfläche haben.
    // t
    // Statemachine for need of Air
    // Perlen in Bereiche einteilen -> z.B. oben-links, oben-rechts, unten-links, unten-rechts


    public DiveAi(Info info) {
        super(info);
        enlistForTournament(583823);
        pearls = new LinkedList(Arrays.asList(info.getScene().getPearl()));
        target = new LinkedList<>();
        graph = new Graph(info.getScene().getObstacles(), info.getScene().getWidth(), info.getScene().getHeight());

        long time = System.currentTimeMillis();
        graph.calculateReflexCorner();
        System.out.println(graph.reflexCorners.size());
        System.out.println(System.currentTimeMillis() - time + " milliseconds");
        sortPearlsLeftToRight();
        //sortPearlsNearest();
        graph.addStartEnd(new Point2D.Float(info.getX(), info.getY()), pearls.get(0));
        path = graph.findPathAStar();
        currentScore = 0;
    }

    private void sortPearlsNearest() {
        pearls.sort((p1, p2) -> {
            double distanceP1 = Math.hypot(p1.getX() - info.getX(), p1.getY() - info.getY());
            double distanceP2 = Math.hypot(p2.getX() - info.getX(), p2.getY() - info.getY());

            return Double.compare(distanceP1, distanceP2);
        });
    }

    private void sortPearlsLeftToRight(){
        pearls.sort((p1, p2) -> {
            double p1X = p1.getX();
            double p2X = p2.getX();

            return  Double.compare(p1X, p2X);
        });
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


    @Override
    public PlayerAction update() {

        float angularAcceleration = avoidCollision(calcPathToTarget());
        float power = info.getMaxAcceleration();

        for (Point2D pearl: pearls) {
            if(currentScore < info.getScore() && info.getX() >= pearl.getX() - 9 && info.getX() <= pearl.getX() + 9 && info.getY() >= pearl.getY() - 9 && info.getY() <= pearl.getY() + 9){
                //System.out.println("Pearl skipped");
                pearls.remove(pearl);
                break;
            }
        }

        return new DivingAction(power, angularAcceleration/5);
    }

    Stack<Integer> path;
    Point2D lastPearl;
    private Point2D calcPathToTarget() {
        if(!path.empty()){
            if (info.getX() >= graph.reflexCorners.get(path.peek()).getX() - 10 && info.getX() <= graph.reflexCorners.get(path.peek()).getX() + 10 && info.getY() >= graph.reflexCorners.get(path.peek()).getY() - 10 && info.getY() <= graph.reflexCorners.get(path.peek()).getY() + 10){
                path.pop();
            }
        }
        if(pearls.size() == 1){
            if(!path.empty()) return graph.reflexCorners.get(path.peek());
            if(lastPearl == null) lastPearl = pearls.get(0);
            return pearls.get(0);
        }
        if(path.empty()){
            currentScore++;
            if (!pearls.isEmpty()) {
                pearls.remove(0);

                sortPearlsNearest();
                Point2D[] tmp = new Point2D[pearls.size()];
                pearls.toArray(tmp);
                Point2D pos = new Point2D.Float(info.getX(), info.getY());
                sortPearlsLeftToRight();
                if (tmp[0].distance(pos) < pearls.get(0).distance(pos) - 25) pearls = Arrays.stream(tmp).collect(Collectors.toList());
            }

            graph.updateStartEnd(new Point2D.Float(info.getX(), info.getY()), pearls.isEmpty() ? lastPearl : pearls.get(0));
            path = graph.findPathAStar();
        }
        return graph.reflexCorners.get(path.peek());
    }



    public VectorF seek(Point2D target){
        Point2D pos = new Point2D.Float(info.getX(), info.getY());
        return new VectorF(pos, target);
    }

    VectorF test;
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

    double breakRadius = 0;
    public float arrive(){
        double distance = Math.sqrt(Math.pow(target.peek().getX() - info.getX(), 2) + Math.pow(target.peek().getY() - info.getY(), 2));
        if(distance <= 0.2f) return 0;
        if(breakRadius < distance) breakRadius = Math.pow(info.getVelocity(),2) / (2 * info.getMaxAcceleration());
        if(distance <= breakRadius){
            return -info.getMaxAcceleration();
        }
        return info.getMaxVelocity();
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
            gfx.fillOval((int) this.graph.reflexCorners.get(i).getX() - 4, (int) this.graph.reflexCorners.get(i).getY() - 4, 10,10);
        }

        // Draw Star/End
//        gfx.setColor(Color.GREEN);
//        gfx.fillOval((int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-1).getX()-5), (int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-1).getY()-5), 10 ,10);
//        gfx.fillOval((int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-2).getX()-5), (int) (this.graph.reflexCorners.get(this.graph.reflexCorners.size()-2).getY()-5), 10 ,10);

        //Draw Path
//        List<Integer> l = path.stream().toList();
//        Point2D p = pearls.get(0);
//        for (int i = 0; i < path.size(); i++) {
//            gfx.drawLine((int) p.getX(), (int) p.getY(), (int) graph.reflexCorners.get(l.get(i)).getX(), (int) graph.reflexCorners.get(l.get(i)).getY());
//            p = graph.reflexCorners.get(l.get(i));
//        }
//        gfx.drawLine((int) info.getX(), (int) info.getY(), (int) graph.reflexCorners.get(l.get(l.size()-1)).getX(), (int) graph.reflexCorners.get(l.get(l.size()-1)).getY());

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