package lando.systems.ld50.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

public class Triangle implements Pool.Poolable {
    public Vector3 p1;
    public Vector3 p2;
    public Vector3 p3;

    public Triangle() {
        this.p1 = new Vector3();
        this.p2 = new Vector3();
        this.p3 = new Vector3();
    }

    Vector3 normal = new Vector3();
    Vector3 AB = new Vector3();
    Vector3 AC = new Vector3();
    public Vector3 getNormal(){
        AB.set(p1).sub(p2);
        AC.set(p1).sub(p3);
        normal.set(AB).crs(AC).nor();
        if (normal.y < 0){
            normal.scl(-1);
        }
        return normal;
    }

    @Override
    public void reset() {

    }

    public Triangle init(Vector3 p1, Vector3 p2, Vector3 p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        return this;
    }
}
