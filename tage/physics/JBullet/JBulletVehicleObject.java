package tage.physics.JBullet;

import com.bulletphysics.collision.shapes.BoxShape;
import javax.vecmath.Vector3f;

public class JBulletVehicleObject extends JBulletPhysicsObject {
    private float[] size;

    public JBulletVehicleObject(int uid, float mass, double[] transform, float[] size){
        super(uid, mass, transform, new BoxShape(new Vector3f(size)), true); 
        this.size = size;
    }
}
