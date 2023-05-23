package s0583823.util;

import java.awt.geom.Point2D;
import java.util.Objects;

public class VectorF {
    public float x;
    public float y;
    public float z;
    public float w;

    public VectorF(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VectorF(float x, float y){
        this.x = x;
        this.y = y;
    }


    public VectorF(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public  VectorF(Point2D start, Point2D end){
        this.x = (float) (end.getX() - start.getX());
        this.y = (float) (end.getY() - start.getY());
    }

    public float magnitude(){
//        if(this.z == null){
//            return (float) Math.sqrt(x*x + y*y);
//        }
        return (float) Math.sqrt(x*x + y*y + z*z);
    }


    public VectorF negate(){
        VectorF out = new VectorF(this.x,this.y,this.z);
        out.x = -this.x;
        out.y = -this.y;
        out.z = -this.z;

        return out;
    }


    public VectorF add(VectorF other){
        VectorF out = new VectorF(this.x,this.y,this.z);
        out.x += other.x;
        out.y += other.y;
        out.z += other.z;

        return out;
    }

    public Point2D.Float addToPoint(Point2D point){
        return new Point2D.Float((float) (point.getX() + this.x), (float) (point.getY() + this.y));
    }

    //Skalarprodukt
    public float dot(VectorF other){
        float out = this.x * other.x + this.y * other.y + this.z * other.z;
        return out;
    }

    public VectorF multiplyScalar(float scalar){
        VectorF out = new VectorF(0,0,0);
        out.x = this.x * scalar;
        out.y = this.y * scalar;
        out.z = this.z * scalar;

        return out;
    }

    public VectorF rotate2D(double angle){
        angle = Math.toRadians(angle);
        VectorF out = new VectorF(0,0);
        out.x = (float) (Math.cos(angle) * this.x + Math.sin(angle) * this.y);
        out.y = (float) (-Math.sin(angle) * this.x + Math.cos(angle) * this.y);
        return out;
    }

    public VectorF multiplyMatrix(Matrix4f matrix){
        VectorF out = new VectorF(this.x,this.y,this.z);
        out.x = matrix.matrix[0][0] * this.x + matrix.matrix[0][1] * this.y + matrix.matrix[0][2] * this.z + matrix.matrix[0][3] * w;
        out.y = matrix.matrix[1][0] * this.x + matrix.matrix[1][1] * this.y + matrix.matrix[1][2] * this.z + matrix.matrix[1][3] * w;
        out.z = matrix.matrix[2][0] * this.x + matrix.matrix[2][1] * this.y + matrix.matrix[2][2] * this.z + matrix.matrix[2][3] * w;

        return out;
    }

    //Kreuzprodukt
    public VectorF cross(VectorF other){
        VectorF out = new VectorF(1,1,1);
        out.x = this.y * other.z - this.z * other.y;
        out.y = this.z * other.x - this.x * other.z;
        out.z = this.x * other.y - this.y * other.x;

        if(out.x == -0){
            out.x = 0;
        }
        if(out.y == -0){
            out.y = 0;
        }
        if(out.z == -0){
            out.z = 0;
        }

        return out;
    }


    public VectorF normalize(){
        VectorF out = new VectorF(this.x,this.y,this.z);
        double amount = Math.sqrt(out.x * out.x + out.y * out.y + out.z * out.z);
        out.x = (float) (this.x / amount);
        out.y = (float) (this.y / amount);
        out.z = (float) (this.z / amount);

        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorF vectorF = (VectorF) o;
        return Float.compare(vectorF.x, x) == 0 && Float.compare(vectorF.y, y) == 0 && Float.compare(vectorF.z, z) == 0 && Float.compare(vectorF.w, w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }
}