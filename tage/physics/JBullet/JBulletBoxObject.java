package tage.physics.JBullet;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.linearmath.Transform;
import javax.vecmath.Vector3f;

public class JBulletBoxObject extends JBulletPhysicsObject {
    private float[] size;

    public JBulletBoxObject(int uid, float mass, double[] transform, float[] size)
    {

        super(uid, mass, transform, new BoxShape(new Vector3f(size)));
        this.size = size;
    }

    public JBulletBoxObject(int uid, float mass, double[] transform, float[] size, Transform transform2)
    {

        super(uid, mass, transform, new BoxShape(new Vector3f(size)), transform2);
        this.size = size;
    }
}
