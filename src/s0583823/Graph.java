package s0583823;


import s0583823.util.VectorF;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;


public class Graph {
    public float graph[][];
    public List<Point2D> reflexCorners;
    public List<Point2D> reflexToAir;
    Path2D obstacles[];
    List<List<Point2D>> obstaclePoints;

    public int indexStart;
    public int indexEnd;

    private int width;
    private int height;

    public Graph(Path2D obstacles[], int width, int height){
        this.obstacles = obstacles;
        this.reflexCorners = new LinkedList<>();
        this.reflexToAir = new LinkedList<>();
        this.obstaclePoints = new ArrayList<>();
        this.width = width;
        this.height = height;
    }


    private Point2D lastPoint = new Point2D.Float(0,0);
    public void calculateReflexCorner(){
        for (int i = 0; i < obstacles.length; i++) {
            List<Point2D> maybeReflex = new LinkedList<>();
            List<Point2D> currentObstaclePoints = new LinkedList<>();
            float[] coords = new float[6];
            PathIterator pathIterator = obstacles[i].getPathIterator(null);
            Point2D points[] = new Point2D[3];
            for (int j = 0; j < 3; j++) {
                pathIterator.currentSegment(coords);
                points[j] = new Point2D.Float(coords[0], coords[1]);
                pathIterator.next();
            }
            currentObstaclePoints.add(points[0]);

            while (!pathIterator.isDone()) {
                currentObstaclePoints.add(points[1]);

                addToReflexCorner(points, maybeReflex);

                pathIterator.currentSegment(coords);
                points[0] = points[1];
                points[1] = points[2];
                points[2] = new Point2D.Float(coords[0], coords[1]);
                pathIterator.next();
            }
            addToReflexCorner(new Point2D[]{points[0], points[1], currentObstaclePoints.get(0)}, maybeReflex);
            addToReflexCorner(new Point2D[]{points[2], currentObstaclePoints.get(0), currentObstaclePoints.get(1)}, maybeReflex);
            currentObstaclePoints.add(points[1]);
            cleanPotentialReflexCorners(maybeReflex);
            reflexCorners.addAll(maybeReflex);
            obstaclePoints.add(currentObstaclePoints);
        }
        calcReflexToAir();
        this.calcGraph();
    }

    private void addToReflexCorner(Point2D points[], List<Point2D> maybeReflex){
        VectorF vec1 = new VectorF(points[0], points[1]);
        VectorF vec2 = new VectorF(points[1], points[2]);


        VectorF normal1 = new VectorF(vec1.y, -vec1.x).normalize();
        float dot = normal1.dot(vec2);


        if(dot < 0 && new VectorF(lastPoint, points[1]).magnitude() > 30 && isInsideLevel(points[1])) {
            VectorF normal2 = new VectorF(vec2.y, -vec2.x).normalize();
            maybeReflex.add(normal2.add(normal1).normalize().multiplyScalar(15).addToPoint(points[1]));
            lastPoint = points[1];
        }
    }

    private void cleanPotentialReflexCorners(List<Point2D> maybeReflex){
        List<Integer> indicesRemove = new LinkedList<>();
        int count = 0;
        for (int i = 0; i < maybeReflex.size(); i++) {
            int index0 = i - 1 > 0 ? i - 1 : maybeReflex.size() - 1;
            int index1 = i + 1 < maybeReflex.size() ? i + 1 : 0;

            VectorF vec1 = new VectorF(maybeReflex.get(index0), maybeReflex.get(i));
            VectorF vec2 = new VectorF(maybeReflex.get(i), maybeReflex.get(index1));

            VectorF normal = new VectorF(vec1.y, -vec1.x).normalize();

            if(normal.dot(vec2) > 0 || vec1.normalize().dot(vec2.normalize()) > 0.997) {
                indicesRemove.add(i - count);
                count++;
            }
        }
        for (int i: indicesRemove) {
            maybeReflex.remove(i);
        }
    }

    private void calcReflexToAir(){
        List<Integer> reflexUnderObstacle = new LinkedList<>();
        List<Integer> reflexBeneathAir = new LinkedList<>();
        for (int i = 0; i < reflexCorners.size(); i++) {
            Point2D p = reflexCorners.get(i);
            if (isObstacleBetween(p, new Point2D.Float((float) p.getX(), 0))){
                reflexUnderObstacle.add(i);
            }else{
                reflexBeneathAir.add(i);
            }
        }
        for (int i = 0; i < reflexUnderObstacle.size(); i++) {
            if(reflexBeneathAir.contains(reflexUnderObstacle.get(i) - 1)) reflexToAir.add(reflexCorners.get(reflexUnderObstacle.get(i) - 1));
            if(reflexBeneathAir.contains(reflexUnderObstacle.get(i) + 1)) reflexToAir.add(reflexCorners.get(reflexUnderObstacle.get(i) + 1));
        }
    }

    private boolean isInsideLevel(Point2D p){
        return p.getX() > 0 && p.getX() < this.width && p.getY() > 0 && p.getY() < this.height;
    }


    private void calcGraph(){
        int threadCount = Thread.activeCount();
        this.graph = new float[reflexCorners.size() + 2][reflexCorners.size() + 2];
        for (int i = 0; i < graph.length; i++) {
            Arrays.fill(graph[i], Float.POSITIVE_INFINITY);
        }
        for (int i = 0; i < graph.length - 2; i++) {
            for (int j = i + 1; j < graph[i].length - 2; j++) {
                final int k = i;
                final int l = j;
                Thread thread = new Thread(() -> {
                    if(!isObstacleBetween(reflexCorners.get(k), reflexCorners.get(l))) {
                        float distance = (float) reflexCorners.get(k).distance(reflexCorners.get(l));
                        graph[k][l] = distance;
                        graph[l][k] = distance;
                    }
                });
                thread.start();
            }
        }
        while (Thread.activeCount() > threadCount){
            //System.out.println("Joining Threads");
        }
    }


    public boolean isObstacleBetween(Point2D p1, Point2D p2){
        Line2D line = new Line2D.Float((float) p1.getX(), (float) p1.getY(), (float) p2.getX(), (float) p2.getY());
        for (int i = 0; i < obstaclePoints.size(); i++) {
            for (int j = 0; j < obstaclePoints.get(i).size()-1; j++) {
                Point2D p3 = new Point2D.Float((float) obstaclePoints.get(i).get(j).getX(), (float) obstaclePoints.get(i).get(j).getY());
                Point2D p4 = new Point2D.Float((float) obstaclePoints.get(i).get(j+1).getX(), (float) obstaclePoints.get(i).get(j+1).getY());
                Line2D obstacleLine = new Line2D.Float((float) p3.getX(), (float) p3.getY(), (float) p4.getX(), (float) p4.getY());
                if (obstacleLine.intersectsLine(line)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void addStartEnd(Point2D start, Point2D end){
        long time = System.currentTimeMillis();
        int threadCount = Thread.activeCount();
        this.reflexCorners.add(start);
        this.reflexCorners.add(end);
        int indexStartEnd = graph.length;
        if(!isObstacleBetween(start, end)){
            float distance = (float) start.distance(end);
            graph[indexStartEnd-2][indexStartEnd-1] = distance;
            graph[indexStartEnd-1][indexStartEnd-2] = distance;
        }

        for (int i = 0; i < graph[i].length - 2; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                if(!isObstacleBetween(start, reflexCorners.get(index))){
                    float distance = (float) start.distance(reflexCorners.get(index));
                    graph[indexStartEnd-2][index] = distance;
                    graph[index][indexStartEnd-2] = distance;
                }
            });
            thread.start();
            thread = new Thread(() -> {
                if(!isObstacleBetween(end, reflexCorners.get(index))){
                    float distance = (float) end.distance(reflexCorners.get(index));
                    graph[indexStartEnd-1][index] = distance;
                    graph[index][indexStartEnd-1] = distance;
                }
            });
            thread.start();
        }
        while (Thread.activeCount() > threadCount){
            //System.out.println("Joining Threads");
        }
        this.indexStart = graph.length - 2;
        this.indexEnd = graph.length - 1;
        System.out.println(System.currentTimeMillis() - time + " milliseconds");
    }

    public void removeStartEnd(){
        this.reflexCorners.remove(reflexCorners.size()-1);
        this.reflexCorners.remove(reflexCorners.size()-1);
        int index = graph.length;

        float tmp[] = new float[index];
        Arrays.fill(tmp, Float.POSITIVE_INFINITY);
        graph[index-1] = tmp.clone();
        graph[index-2] = tmp.clone();

        for (int i = 0; i < graph.length; i++) {
            graph[i][index-1] = Float.POSITIVE_INFINITY;
            graph[i][index-2] = Float.POSITIVE_INFINITY;
        }
        this.indexStart = -1;
        this.indexEnd = - 1;
    }


    public void updateStartEnd(Point2D start, Point2D end){
        this.removeStartEnd();
        this.addStartEnd(start, end);
    }


    public Stack<Integer> findPathAStar(){
        Stack<Integer> out = new Stack<>();
        List<Node> queue = new LinkedList<>();
        queue.add(new Node(indexStart, null, 0, (float) reflexCorners.get(indexStart).distance(reflexCorners.get(indexEnd))));
        List<Node> visited = new LinkedList<>();

        while (!queue.isEmpty()){
            Node current = queue.get(0);
            queue.remove(0);
            if(current.index == indexEnd) {
                while (current.previous != null){
                    out.push(current.index);
                    current = current.previous;
                }
                return out;
            }
            for (int i = 0; i < reflexCorners.size(); i++) {
                if (!visited.contains(current) && Float.isFinite(graph[current.index][i])) {
                    float cost = current.previous != null ? graph[current.index][i] + current.distanceToPrev : graph[current.index][i];
                    Node n = new Node(i, current, cost, (float) reflexCorners.get(i).distance(reflexCorners.get(indexEnd)));
                    if(visited.contains(n)) continue;
                    if(!queue.contains(n)) queue.add(n);
                    else {
                        if(queue.get(queue.indexOf(n)).distanceToPrev > n.distanceToPrev) {
                            queue.set(queue.indexOf(n), n);
                        }
                    }
                }
            }

            visited.add(current);
            queue.sort((o1, o2) -> Float.compare(o1.distanceToPrev + o1.previous.distanceToPrev + o1.distanceToEnd, o2.distanceToPrev + o2.previous.distanceToPrev + o2.distanceToEnd));
        }
        return null;
    }



    private class Node {
        public int index;
        public Node previous;
        public float distanceToPrev;
        public float distanceToEnd;

        public Node(int index, Node previous, float distanceToPrev, float distanceToEnd){
            this.index = index;
            this.previous = previous;
            this.distanceToPrev = distanceToPrev;
            this.distanceToEnd = distanceToEnd;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return index == node.index && distanceToEnd == node.distanceToEnd;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, distanceToEnd);
        }

        @Override
        public String toString() {
            String prev = this.previous != null ? "" + previous.index : "null";
            return "Node{" +
                    "index=" + index +
                    ", previous=" + prev +
                    ", distanceToPrev=" + distanceToPrev +
                    ", distanceToEnd=" + distanceToEnd +
                    '}'+
                    "\n";
        }
    }





    // Debugging/Testing
    public static void main(String[] args) {
        Path2D path1 = new Path2D.Float();
        path1.moveTo(250,500);
        path1.lineTo(350,400);
        path1.lineTo(450,400);
        path1.lineTo(550,500);
        path1.lineTo(550,600);
        path1.lineTo(450,700);
        path1.lineTo(350,700);
        path1.lineTo(250,600);
        path1.closePath();

        Path2D path2 = new Path2D.Float();
        path2.moveTo(550,250);
        path2.lineTo(900,250);
        path2.lineTo(900,600);
        path2.lineTo(750, 550);
        path2.lineTo(700, 350);
        path2.closePath();

        Path2D obstacles[] = new Path2D[]{path1, path2};
        List<List<Point2D>> obstaclePoints = new ArrayList<>();

        for (int i = 0; i < obstacles.length; i++) {
            List<Point2D> points = new LinkedList();
            float[] coords = new float[6];
            PathIterator pathIterator = obstacles[i].getPathIterator(null);
            while (!pathIterator.isDone()) {
                pathIterator.currentSegment(coords);
                points.add(new Point2D.Float(coords[0], coords[1]));
                pathIterator.next();
            }
            obstaclePoints.add(points);
        }

        Graph graph = new Graph(obstacles, 1000, 1000);
        graph.calculateReflexCorner();
        graph.addStartEnd(new Point2D.Float(200, 300), new Point2D.Float(850,750));

        //graph.updateStartEnd(new Point2D.Float(150, 600), new Point2D.Float(950,200), obstacles);
        System.out.println(graph.findPathAStar());
        Stack<Integer> stack = graph.findPathAStar();
        stack.push(graph.indexStart);
        List<Integer> s = stack.stream().toList();
//        System.out.println(s);


        JFrame frame = new JFrame();
        frame.setBounds(0,0,1000,1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(){
            @Override
            public void paint(Graphics graphics){
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                AffineTransform mirror = new AffineTransform();
                mirror.scale(1,-1);
                mirror.translate(0,-getHeight());
                //g.setTransform(mirror);

                g.setColor(Color.BLUE);
                g.fill(path1);
                g.setColor(Color.WHITE);
                g.fill(path2);
                g.setColor(Color.GREEN);
                g.fillOval((int) (graph.reflexCorners.get(graph.indexStart).getX()-8), (int) (graph.reflexCorners.get(graph.indexStart).getY()-8),16,16);
                g.fillOval((int) (graph.reflexCorners.get(graph.indexEnd).getX()-8), (int) (graph.reflexCorners.get(graph.indexEnd).getY()-8),16,16);


                for (int i = 0; i < obstaclePoints.get(0).size(); i++) {
                    g.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
                    g.fillOval((int) obstaclePoints.get(0).get(i).getX()-5, (int) obstaclePoints.get(0).get(i).getY()-5, 10, 10);
                }

                for (int i = 0; i < graph.reflexCorners.size(); i++) {
                    g.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
                    g.fillOval((int)graph.reflexCorners.get(i).getX()-5, (int) graph.reflexCorners.get(i).getY()-5, 10, 10);
                }


                g.setColor(Color.GREEN);
                for (int i = 0; i < graph.graph.length; i++) {
                    for (int j = i; j < graph.graph.length; j++) {
                        if(graph.graph[i][j] < Float.POSITIVE_INFINITY){
                            Point2D p1 = new Point2D.Float((float) graph.reflexCorners.get(i).getX(), (float) graph.reflexCorners.get(i).getY());
                            Point2D p2 = new Point2D.Float((float) graph.reflexCorners.get(j).getX(), (float) graph.reflexCorners.get(j).getY());
                            g.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
                        }
                    }
                }

                g.setColor(Color.RED);
                for (int i = 0; i < s.size()-1; i++) {
                    g.drawLine((int) graph.reflexCorners.get(s.get(i)).getX(), (int) graph.reflexCorners.get(s.get(i)).getY(), (int) graph.reflexCorners.get(s.get(i+1)).getX(), (int) graph.reflexCorners.get(s.get(i+1)).getY());
                }

                for (Point2D p: graph.reflexToAir) {
                    g.fillOval((int) p.getX(), (int) p.getY(), 15, 15);
                }
            }
        };
        frame.setBackground(Color.GRAY);
        frame.add(panel);
        frame.setVisible(true);
    }
}