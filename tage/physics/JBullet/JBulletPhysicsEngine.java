package tage.physics.JBullet;

import java.util.HashMap;
import java.util.Vector;

import javax.vecmath.Vector3f;

import tage.physics.PhysicsBallSocketConstraint;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsHingeConstraint;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.bulletphysics.dynamics.vehicle.WheelInfo;

// For debug purposes
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.collision.shapes.CompoundShape;
import org.joml.Matrix4f;

/**
 * This class provides an implementation of the PhysicsEngine interface using the JBullet physics engine.
 * 
 * @author Russell Bolles
 * @author John Clevenger (JavaDoc)
 * @author Oscar Solorzano
 */

public class JBulletPhysicsEngine implements PhysicsEngine {
	// maximum number of objects (and allow user to shoot additional boxes)
		private static final int MAX_PHYSICS_OBJECTS = 1024;
		private static int nextUID;

		// private VehicleRaycaster vehicleRaycaster;
		// private RaycastVehicle vehicle;
		private HashMap<Integer, RaycastVehicle> vehicles;
		private HashMap<Integer, VehicleTuning> vehicleTunings;
		// private VehicleTuning myVehicleTuning;
		private DefaultCollisionConfiguration collisionConfiguration;
		private CollisionDispatcher dispatcher;
		private SequentialImpulseConstraintSolver solver;
		private AxisSweep3 overlappingPairCache;
		private DiscreteDynamicsWorld dynamicsWorld;
		// keep the collision shapes, for deletion/cleanup
		private Vector<PhysicsObject> objects;

		static
		{
			PhysicsEngineFactory.registerPhysicsEngine("tage.physics.JBullet.JBulletPhysicsEngine", JBulletPhysicsEngine.class);
		}
		
		/**
		 * {@code #initSystem()} initializes the underlying physics engine, providing an (empty)
		 * "physics world" along with a default collision handler, collision dispatcher, and constraint
		 * solver. The default physics world extents are {-10,000 ... 10,000} in each of X, Y, and Z;
		 * object locations should be constrained to these bounds for proper physics calculations. The
		 * implementation's default gravity vector in the physics world is [0,0,0] 
		 * (meaning gravity is turned off by default).  Note that this means the implementation 
		 * <I>does not use the DEFAULT_GRAVITY constants defined in {@link PhysicsEngine}</i>.
		 */
		public void initSystem() {
			// collision configuration contains default setup for memory, collision setup
			collisionConfiguration = new DefaultCollisionConfiguration();

			// use the default collision dispatcher. For parallel processing you can use a diffent
			// dispatcher (see Extras/BulletMultiThreaded)
			dispatcher = new CollisionDispatcher(collisionConfiguration);

			// the maximum size of the collision world. Make sure objects stay within these boundaries
			// TODO: AxisSweep3
			// Don't make the world AABB size too large, it will harm simulation quality and performance
			Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
			Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);
			overlappingPairCache = new AxisSweep3(worldAabbMin, worldAabbMax, MAX_PHYSICS_OBJECTS);
			// overlappingPairCache = new SimpleBroadphase(MAX_PROXIES);

			// the default constraint solver. For parallel processing you can use a different solver
			// (see Extras/BulletMultiThreaded)
			SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
			solver = sol;

			// TODO: needed for SimpleDynamicsWorld
			// sol.setSolverMode(sol.getSolverMode() & ~SolverMode.SOLVER_CACHE_FRIENDLY.getMask());

			dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver,
					collisionConfiguration);

			// dynamicsWorld = new SimpleDynamicsWorld(dispatcher, overlappingPairCache, solver,
			// collisionConfiguration);

			// float[] gravity_vector =
			// {PhysicsEngine.DEFAULT_GRAVITY_X,PhysicsEngine.DEFAULT_GRAVITY_Y,PhysicsEngine.DEFAULT_GRAVITY_Z};
			float[] gravity_vector = { 0, 0, 0 };
			setGravity(gravity_vector);

			objects = new Vector<PhysicsObject>(50, 25);

			vehicles = new HashMap<>();
			vehicleTunings = new HashMap<>();
		}
		
		/*
		public void createPoint2PointConstraint(PhysicsObject object1, PhysicsObject object2, double[] aPivot, double[] bPivot){
			RigidBody rb1, rb2;
			rb1 = ((JBulletPhysicsObject) object1).getRigidBody();
			rb2 = ((JBulletPhysicsObject) object2).getRigidBody();
			Vector3f aPivotVec = new Vector3f(JBulletUtils.double_to_float_array(aPivot));
			Vector3f bPivotVec = new Vector3f(JBulletUtils.double_to_float_array(bPivot));
					
			Point2PointConstraint constraint = new Point2PointConstraint(rb1,rb2,aPivotVec,bPivotVec);
			rb1.addConstraintRef(constraint);
			rb2.addConstraintRef(constraint);
			dynamicsWorld.addConstraint(constraint);
		}
		*/
		
		/**
		 * Sets the gravity vector for the physics world to the specified [x,y,z] vector.
		 */
		public void setGravity(float[] gravity_vector) {
			dynamicsWorld.setGravity(new Vector3f(gravity_vector));
		}

		/**
		 * Adds a {@link PhysicsObject} object of type Box to the physics world.
		 */
		public PhysicsObject addBoxObject(int uid, float mass, double[] transform, float[] size) {
			// PhysicsEngine asks for dimensions, JBullet uses halfExtents
			float[] temp = new float[size.length];
			for(int i = 0; i < size.length; i++)
			{
				temp[i] = size[i]/2f;
			}
			JBulletBoxObject boxObject = new JBulletBoxObject(uid, mass, transform, temp);
			this.dynamicsWorld.addRigidBody(boxObject.getRigidBody());
			this.objects.add(boxObject);
			return boxObject;
		}

		/**
		 * Adds a {@link PhysicsObject} object of type Sphere to the physics world.
		 */
		public PhysicsObject addSphereObject(int uid, float mass, double[] transform, float radius) {
			JBulletSphereObject sphereObject = new JBulletSphereObject(uid, mass, transform, radius);
			this.dynamicsWorld.addRigidBody(sphereObject.getRigidBody());
			this.objects.add(sphereObject);
			return sphereObject;
		}

		/**
		 * Add a Cone object to the physics worlds, with the tip at (0,0,0)
		 * 
		 * @param mass
		 * @param radius
		 * @param height
		 */
		public PhysicsObject addConeObject(int uid, float mass, double[] transform, float radius,
				float height) {
			JBulletConeObject coneObject = new JBulletConeObject(uid, mass, transform, radius, height);
			this.dynamicsWorld.addRigidBody(coneObject.getRigidBody());
			this.objects.add(coneObject);
			return coneObject;
		}

		public PhysicsObject addConeXObject(int uid, float mass, double[] transform, float radius,
				float height) {
			JBulletConeXObject coneObject = new JBulletConeXObject(uid, mass, transform, radius, height);
			this.dynamicsWorld.addRigidBody(coneObject.getRigidBody());
			this.objects.add(coneObject);
			return coneObject;
		}

		public PhysicsObject addConeZObject(int uid, float mass, double[] transform, float radius,
				float height) {
			JBulletConeZObject coneObject = new JBulletConeZObject(uid, mass, transform, radius, height);
			this.dynamicsWorld.addRigidBody(coneObject.getRigidBody());
			this.objects.add(coneObject);
			return coneObject;
		}

		/**
		 * Add a cylinder object to the physics simulation
		 * 
		 * @param mass
		 * @param transform
		 * @param halfExtents
		 *            the dimensions of the height, width, and length
		 */
		public PhysicsObject addCylinderObject(int uid, float mass, double[] transform,
				float[] halfExtents) {
			JBulletCylinderObject cylinderObject = new JBulletCylinderObject(uid, mass, transform,
					halfExtents);
			this.dynamicsWorld.addRigidBody(cylinderObject.getRigidBody());
			this.objects.add(cylinderObject);
			return cylinderObject;
		}

		/**
		 * Add a cylinder object to the physics simulation
		 * 
		 * @param mass
		 * @param transform
		 * @param halfExtents
		 *            the dimensions of the height, width, and length
		 */
		public PhysicsObject addCylinderXObject(int uid, float mass, double[] transform,
				float[] halfExtents) {
			JBulletCylinderXObject cylinderObject = new JBulletCylinderXObject(uid, mass, transform,
					halfExtents);
			this.dynamicsWorld.addRigidBody(cylinderObject.getRigidBody());
			this.objects.add(cylinderObject);
			return cylinderObject;
		}

		/**
		 * Add a cylinder object to the physics simulation
		 * 
		 * @param mass
		 * @param transform
		 * @param halfExtents
		 *            the dimensions of the height, width, and length
		 */
		public PhysicsObject addCylinderZObject(int uid, float mass, double[] transform,
				float[] halfExtents) {
			JBulletCylinderZObject cylinderObject = new JBulletCylinderZObject(uid, mass, transform,
					halfExtents);
			this.dynamicsWorld.addRigidBody(cylinderObject.getRigidBody());
			this.objects.add(cylinderObject);
			return cylinderObject;
		}

		/**
		 * Add a capsule object to the physics simulation
		 * 
		 * @param mass
		 * @param transform
		 * @param radius
		 * @param height
		 */
		public PhysicsObject addCapsuleObject(int uid, float mass, double[] transform, float radius,
				float height) {
			JBulletCapsuleObject cylinderObject = new JBulletCapsuleObject(uid, mass, transform,
					radius, height);
			this.dynamicsWorld.addRigidBody(cylinderObject.getRigidBody());
			this.objects.add(cylinderObject);
			return cylinderObject;

		}

		/**
		 * Add a capsule object to the physics simulation
		 * 
		 * @param mass
		 * @param transform
		 * @param radius
		 * @param height
		 */
		public PhysicsObject addCapsuleXObject(int uid, float mass, double[] transform, float radius,
				float height) {
			JBulletCapsuleXObject cylinderObject = new JBulletCapsuleXObject(uid, mass, transform,
					radius, height);
			this.dynamicsWorld.addRigidBody(cylinderObject.getRigidBody());
			this.objects.add(cylinderObject);
			return cylinderObject;

		}

		/**
		 * Add a capsule object to the physics simulation
		 * 
		 * @param mass
		 * @param transform
		 * @param radius
		 * @param height
		 */
		public PhysicsObject addCapsuleZObject(int uid, float mass, double[] transform, float radius,
				float height) {
			JBulletCapsuleZObject cylinderObject = new JBulletCapsuleZObject(uid, mass, transform,
					radius, height);
			this.dynamicsWorld.addRigidBody(cylinderObject.getRigidBody());
			this.objects.add(cylinderObject);
			return cylinderObject;
		}

		public PhysicsObject addVehicleObject(int uid, float mass, double[] xform, float[] size){
			
			JBulletVehicleObject vehicleObject = new JBulletVehicleObject(uid, mass, xform, size);
			
			this.dynamicsWorld.addRigidBody(vehicleObject.getRigidBody());
			this.objects.add(vehicleObject);

		    VehicleRaycaster vehicleRaycaster = new DefaultVehicleRaycaster(this.dynamicsWorld);

			VehicleTuning myVehicleTuning = new VehicleTuning();

			RaycastVehicle vehicle = new RaycastVehicle(myVehicleTuning, vehicleObject.getRigidBody() , vehicleRaycaster);
			vehicle.setCoordinateSystem(0, 1, 2);
	
			//Disable Deactivation
			vehicle.getRigidBody().setActivationState(CollisionObject.DISABLE_DEACTIVATION);
			
			dynamicsWorld.addVehicle(vehicle);
			vehicles.put(uid, vehicle);
			vehicleTunings.put(uid, myVehicleTuning);

			return vehicleObject;
		}

		public RaycastVehicle getVehicle(int uid){
			return vehicles.get(uid);
		}

		public VehicleTuning getVehicleTuning(int uid){
			return vehicleTunings.get(uid);
		}

		public void addWheels(RaycastVehicle myVehicle, VehicleTuning tuning, float[] chassisHalfExtens, float wheelRadius, float connectionHeight, float wheelWidth) {
			Vector3f wheelDirectionCS0 = new Vector3f(0, -1, 0);
			Vector3f wheelAxleCS = new Vector3f(-1, 0, 0);
			float suspensionRestLength = 0.6f;
	
			// Adds the front wheels
			Vector3f wheelConnectionPoint = new Vector3f(chassisHalfExtens[0] - wheelRadius, connectionHeight,
					chassisHalfExtens[2] - wheelWidth);
			myVehicle.addWheel(wheelConnectionPoint, wheelDirectionCS0, wheelAxleCS, suspensionRestLength,
					wheelRadius, tuning, true);
	
			wheelConnectionPoint = new Vector3f(-chassisHalfExtens[0] + wheelRadius, connectionHeight,
					chassisHalfExtens[2] - wheelWidth);
			myVehicle.addWheel(wheelConnectionPoint, wheelDirectionCS0, wheelAxleCS, suspensionRestLength,
					wheelRadius, tuning, true);
	
			// Adds the rear wheels
			wheelConnectionPoint = new Vector3f(-chassisHalfExtens[0] + wheelRadius, connectionHeight,
					(-chassisHalfExtens[2]) + wheelWidth);
			myVehicle.addWheel(wheelConnectionPoint, wheelDirectionCS0, wheelAxleCS, suspensionRestLength,
					wheelRadius, tuning, false);
	
			wheelConnectionPoint = new Vector3f(chassisHalfExtens[0] - wheelRadius, connectionHeight,
					(-chassisHalfExtens[2]) + wheelWidth);
			myVehicle.addWheel(wheelConnectionPoint, wheelDirectionCS0, wheelAxleCS, suspensionRestLength,
					wheelRadius, tuning, false);
	
			// Edit wheel info for all 4 wheels
			for (int i = 0; i < 4; i++)
			{
				WheelInfo wheel = myVehicle.getWheelInfo(i);
				wheel.suspensionStiffness = 20f;
				wheel.wheelsDampingCompression = 4.4f;
				wheel.wheelsDampingRelaxation = 2.3f;
				wheel.frictionSlip = 1000f;
				wheel.rollInfluence = 0.1f;
			}
		}

		public PhysicsObject addStaticPlaneObject(int uid, double[] transform, float[] up_vector,
				float plane_constant) {
			JBulletStaticPlaneObject planeObject = new JBulletStaticPlaneObject(uid, transform,
					up_vector, plane_constant);
			this.dynamicsWorld.addRigidBody(planeObject.getRigidBody());
			this.objects.add(planeObject);
			return planeObject;
		}

		/**
		 * Removes the {@link PhysicsObject} with the specified UID from the physics world, if it 
		 * exists in the world.  This method has no effect if no object with the specified UID
		 * exists in the physics world.
		 */
		public void removeObject(int uid) {
			JBulletPhysicsObject target_object = null;
			for (PhysicsObject object : objects) {
				if (object.getUID() == uid) {
					target_object = (JBulletPhysicsObject) object;
				}
			}
			if (target_object != null) {
				dynamicsWorld.removeRigidBody(target_object.getRigidBody());
			}
		}

		/**
		 * Forces the physics world to advance (that is, steps the physics simulation) by the 
		 * specified amount of time, given in nanoseconds.
		 */
		public void update(float nanoseconds) {
			if (dynamicsWorld != null) {
				dynamicsWorld.stepSimulation(nanoseconds/1000f);//, 4, 1f/60f);
			}
		}

		/**
		 * Returns a unique ID used to identify physics objects.
		 */
		public int nextUID() {
			int temp = JBulletPhysicsEngine.nextUID;
			JBulletPhysicsEngine.nextUID++;
			return temp;

		}
		
		@Override
		public PhysicsHingeConstraint addHingeConstraint(int uid, PhysicsObject bodyA, PhysicsObject bodyB, float axisX, float axisY, float axisZ){
			JBulletHingeConstraint hingeConstraint = new JBulletHingeConstraint(uid, (JBulletPhysicsObject)bodyA, (JBulletPhysicsObject)bodyB, axisX, axisY, axisZ);
			dynamicsWorld.addConstraint(hingeConstraint.getConstraint());
			return hingeConstraint;
		}

		// @Override
		// public PhysicsHingeConstraint addHingeConstraint(int uid, PhysicsObject bodyA, PhysicsObject bodyB, float axisX, float axisY, float axisZ, Vector3f pivotOffsetA, Vector3f pivotOffsetB) {
		// 	JBulletHingeConstraint hingeConstraint = new JBulletHingeConstraint(uid, (JBulletPhysicsObject) bodyA, (JBulletPhysicsObject) bodyB, axisX, axisY, axisZ, pivotOffsetA, pivotOffsetB);
		// 	dynamicsWorld.addConstraint(hingeConstraint.getConstraint());
		// 	return hingeConstraint;
		// }

		@Override
		public PhysicsBallSocketConstraint addBallSocketConstraint(int uid, PhysicsObject bodyA, PhysicsObject bodyB) {
			JBulletBallSocketConstraint ballSocketConstraint = new JBulletBallSocketConstraint(uid, (JBulletPhysicsObject)bodyA, (JBulletPhysicsObject)bodyB);
			dynamicsWorld.addConstraint(ballSocketConstraint.getConstraint());
			return ballSocketConstraint;
		}
	
		@Override
		public DiscreteDynamicsWorld getDynamicsWorld() { return dynamicsWorld; }
}

	/* // Start of BoxShape Debug Code
	// Create the first cube
	float halfExtent = 0.1f;
	Vector3f position1 = new Vector3f(0.1f, 0.1f, 0.1f);
	CollisionShape cubeShape1 = new BoxShape(new Vector3f(halfExtent, halfExtent, halfExtent));
	Matrix4f transformMatrix1 = new Matrix4f().translate(0.0f, 5f, 0.0f);
	Transform transform1 = new Transform(convertJomlToJavax(transformMatrix1));
	transform1.setIdentity();
	transform1.origin.set(position1);

	DefaultMotionState motionState1 = new DefaultMotionState(transform1);
	RigidBodyConstructionInfo rigidBodyCI1 = new RigidBodyConstructionInfo(0, motionState1, cubeShape1, new Vector3f(0, 0, 0));
	RigidBody cube1 = new RigidBody(rigidBodyCI1);

	// Create the second cube
	Vector3f position2 = new Vector3f(0.1f, 0.1f, 0.1f);
	CollisionShape cubeShape2 = new BoxShape(new Vector3f(halfExtent, halfExtent, halfExtent));
	Transform transform2 = new Transform();
	transform2.setIdentity();
	transform2.origin.set(position2);
	transform2.origin.add(new javax.vecmath.TexCoord3f(0, 1, 0));

	DefaultMotionState motionState2 = new DefaultMotionState(transform2);
	RigidBodyConstructionInfo rigidBodyCI2 = new RigidBodyConstructionInfo(0, motionState2, cubeShape2, new Vector3f(0, 0, 0));
	RigidBody cube2 = new RigidBody(rigidBodyCI2);

	// Add the cubes to the dynamics world
	dynamicsWorld.addRigidBody(cube1);
	dynamicsWorld.addRigidBody(cube2);

	// End of BoxShape Debug Code */