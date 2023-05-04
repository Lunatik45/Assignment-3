package tage.physics.JBullet;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import tage.physics.PhysicsHingeConstraint;

public class JBulletHingeConstraint extends JBulletConstraint implements PhysicsHingeConstraint {
	private HingeConstraint hingeConstraint;
	private float[] axis;
	// private Vector3f pivotOffsetA;
    // private Vector3f pivotOffsetB;

    // public JBulletHingeConstraint(int uid, JBulletPhysicsObject bodyA, JBulletPhysicsObject bodyB, float axisX, float axisY, float axisZ, Vector3f pivotOffsetA, Vector3f pivotOffsetB) {
    //     super(uid, bodyA, bodyB);
    //     RigidBody rigidA = bodyA.getRigidBody();
    //     RigidBody rigidB = bodyB.getRigidBody();
    //     axis = new float[]{axisX, axisY, axisZ};
    //     this.pivotOffsetA = pivotOffsetA;
    //     this.pivotOffsetB = pivotOffsetB;
    //     Vector3f pivotInA = new Vector3f(pivotOffsetA);
    //     Vector3f pivotInB = new Vector3f(pivotOffsetB);
    //     pivotInA.negate();
    //     pivotInB.negate();
    //     pivotInA.add(getCenterOfMass(rigidA));
    //     pivotInB.add(getCenterOfMass(rigidB));
    //     hingeConstraint = new HingeConstraint(rigidA, rigidB, pivotInA, pivotInB, new Vector3f(axisX, axisY, axisZ), new Vector3f(axisX, axisY, axisZ));
    //     rigidA.addConstraintRef(hingeConstraint);
    //     rigidB.addConstraintRef(hingeConstraint);
    // }

	public JBulletHingeConstraint(int uid, JBulletPhysicsObject bodyA, JBulletPhysicsObject bodyB, float axisX, float axisY, float axisZ) {
		super(uid, bodyA, bodyB);
		RigidBody rigidA = bodyA.getRigidBody();
		RigidBody rigidB = bodyB.getRigidBody();
		float []pivotInA = new float[]{0, 0, 0};
		float []pivotInB = new float[]{(float) (bodyA.getTransform()[12]-bodyB.getTransform()[12]),(float) (bodyA.getTransform()[13]-bodyB.getTransform()[13]),(float) (bodyA.getTransform()[14]-bodyB.getTransform()[14])};
		axis = new float[]{axisX, axisY, axisZ};
		Vector3f pivotInAa = new Vector3f(pivotInA);
		Vector3f pivotInBb = new Vector3f(pivotInB);
		pivotInAa.negate();
		pivotInBb.negate();
		hingeConstraint = new HingeConstraint(rigidA, rigidB, pivotInBb, pivotInAa , new Vector3f(axisX, axisY, axisZ), new Vector3f(axisX, axisY, axisZ));
		rigidA.addConstraintRef(hingeConstraint);
		rigidB.addConstraintRef(hingeConstraint);
		hingeConstraint.setLimit(0f, 0f);
	}

	/**
	 * Returns the JBullet specific hinge constraint
	 * @return The hinge constraint as a JBullet HingeConstraint
	 */
	public HingeConstraint getConstraint(){
		return hingeConstraint;
	}

	/**
	 * Gets the center of mass of the physics object in world space.
	 * @return A Vector3f representing the center of mass
	 */
	public Vector3f getCenterOfMass(RigidBody rigidBody) {
		Transform transform = new Transform();
		rigidBody.getMotionState().getWorldTransform(transform);
		return transform.origin;
	}

	@Override
	public float getAngle() {
		return hingeConstraint.getHingeAngle();
	}

	@Override
	public float[] getAxis() {
		return axis;
	}

	
    @Override
    public void setLimits(float low, float high) {
        hingeConstraint.setLimit(low, high);
    }

    @Override
    public void enableMotor(boolean enable, float targetVelocity, float maxMotorImpulse) {
        hingeConstraint.enableAngularMotor(enable, targetVelocity, maxMotorImpulse);
    }
}
