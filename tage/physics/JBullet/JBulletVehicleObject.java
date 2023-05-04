package tage.physics.JBullet;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;

import javax.vecmath.Vector3f;

public class JBulletVehicleObject extends JBulletPhysicsObject {
    private float[] size;

    public JBulletVehicleObject(int uid, float mass, double[] xform, float[] size, DynamicsWorld dw){
        super(uid, mass, xform, new BoxShape(new Vector3f(size)), true, dw); 
        this.size = size;
    }
}
