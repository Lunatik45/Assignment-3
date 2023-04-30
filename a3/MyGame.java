package a3;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JToggleButton.ToggleButtonModel;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.bulletphysics.dynamics.vehicle.WheelInfo;

import net.java.games.input.Component.Identifier;
import tage.Camera;
import tage.CameraOrbit3D;
import tage.Engine;
import tage.GameObject;
import tage.Light;
import tage.Log;
import tage.ObjShape;
import tage.RenderSystem;
import tage.SpringCameraController;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.Viewport;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
import tage.input.action.AccelAction;
import tage.input.action.DecelAction;
import tage.input.action.TurnLeftAction;
import tage.input.action.TurnRightAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.shapes.ImportedModel;
import tage.shapes.Sphere;
import tage.shapes.TerrainPlane;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import tage.physics.PhysicsHingeConstraint;
import com.bulletphysics.dynamics.constraintsolver.HingeConstraint;

// Import other shapes as needed

/**
 * Assignment 3
 * <p>
 * CSC 165-02
 * <p>
 * Tested on:
 * 
 * @author Roger Chavez, Eric Rodriguez
 */
public class MyGame extends VariableFrameRateGame {

	private static Engine engine;

	private CameraOrbit3D orbitController;
	private SpringCameraController springController;
	private File scriptFile;
	private GameObject avatar, terrain, terrainQ1, terrainQ2, terrainQ3, terrainQ4, trafficCone, myRoad, frontRW, frontLW, backRW, backLW;
	private GhostManager ghostManager;
	private InputManager im;
	private Light light;
	private ObjShape ghostShape, dolphinShape, terrainShape, terrainQ1S, terrainQ2S, terrainQ3S, terrainQ4S, trafficConeShape, boxCarShape, myRoadShape, frontRWShape, frontLWShape, backRWShape, backLWShape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private Robot robot;
	private ScriptEngine jsEngine;
	private String serverAddress;
	private TextureImage dolphinTex, ghostTex, terrainTex, trafficConeTex, boxCarTex, myRoadTex;
	private TextureImage terrainHeightMap, terrainHeightMap1, terrainHeightMap2, terrainHeightMap3, terrainHeightMap4;
	private float vals[] = new float[16];

	private boolean isClientConnected = false;
	private boolean isFalling = false, mouseIsRecentering, updateScriptInRuntime, allowLogLevelChange;
	private double centerX, centerY, prevMouseX, prevMouseY, curMouseX, curMouseY;
	private double acceleration, deceleration, stoppingForce, gravity, speed = 0, gravitySpeed = 0, turnConst, turnCoef;
	private double startTime, prevTime, elapsedTime, amt;
	private float elapsed;
	private int lakeIslands;
	private int maxSpeed;
	private int passes = 0;
	private int serverPort;
	private PhysicsEngine physicsEngine;
	private PhysicsObject avatarP, trafficConeP, terrainP, frontRWP, frontLWP, backRWP, backLWP;
	private PhysicsHingeConstraint frontRWHinge, frontLWHinge, backRWHinge, backLWHinge;
	private Boolean toggleCamaraType = false;
	private Boolean rotatingWheels = false;

	public MyGame(String serverAddress, int serverPort, String protocol, int debug)
	{
		super();

		Log.setLogLevel(debug);
		ghostManager = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;

		ScriptEngineManager factory = new ScriptEngineManager();
		jsEngine = factory.getEngineByName("js");
		scriptFile = new File("assets/scripts/params.js");
		updateScripts();

		if (updateScriptInRuntime)
		{
			System.out.println(
					"Note: Script will update during runtime.\nCAUTION: Performance may be affected while this mode is in use");
		}
	}

	public static void main(String[] args)
	{
		MyGame game;
		if (args.length == 3)
		{
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2], 0);
		} else if (args.length == 4)
		{
			game = new MyGame(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
		} else
		{
			String msg = String.format(
					"Invalid number of arguements.\nFormat: java %s SERVER_ADDRESS SERVER_PORT SERVER_PROTOCOL [DEBUG_LEVEL]",
					MyGame.class.getName());
			throw new IllegalArgumentException(msg);
		}
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{
		ghostShape = new Sphere();
		dolphinShape = new ImportedModel("dolphinHighPoly.obj");
		trafficConeShape = new ImportedModel("trafficCone.obj");
		terrainShape = new TerrainPlane(100);
		// terrainQ1S = new TerrainPlane(25);
		// terrainQ2S = new TerrainPlane(25);
		// terrainQ3S = new TerrainPlane(25);
		// terrainQ4S = new TerrainPlane(25);
		myRoadShape = new ImportedModel("myRoad.obj");
		boxCarShape = new ImportedModel("box_car.obj");
		backRWShape = new ImportedModel("BackRightWheel.obj");
		frontRWShape = new ImportedModel("FrontRightWheel.obj");
		backLWShape = new ImportedModel("BackLeftWheel.obj");
		frontLWShape = new ImportedModel("FrontLeftWheel.obj");
	}

	@Override
	public void loadTextures()
	{
		dolphinTex = new TextureImage("Dolphin_HighPolyUV.png");
		trafficConeTex = new TextureImage("traffic_cone.png");
		ghostTex = new TextureImage("redDolphin.jpg");
		terrainTex = new TextureImage("tileable_grass_01.png");
		terrainHeightMap = new TextureImage("terrain1.jpg");
		terrainHeightMap1 = new TextureImage("terrain1_1.jpg");
		terrainHeightMap2 = new TextureImage("terrain1_2.jpg");
		terrainHeightMap3 = new TextureImage("terrain1_3.jpg");
		terrainHeightMap4 = new TextureImage("terrain1_4.jpg");
		boxCarTex = new TextureImage("CarTexture.png");
		myRoadTex = new TextureImage("road1.jpg");
	}

	@Override
	public void loadSkyBoxes()
	{
		lakeIslands = (engine.getSceneGraph()).loadCubeMap("lakeIslands");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(lakeIslands);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{
		// avatar = new GameObject(GameObject.root(), boxCarShape, boxCarTex);
		avatar = new GameObject(GameObject.root(), boxCarShape);
		avatar.getRenderStates().setWireframe(true);

		backRW = new GameObject(avatar, backRWShape, boxCarTex);
		backLW = new GameObject(avatar, backLWShape, boxCarTex);
		frontRW = new GameObject(avatar, frontRWShape, boxCarTex);
		frontLW = new GameObject(avatar, frontLWShape, boxCarTex);

		// myRoad = new GameObject(GameObject.root(), myRoadShape, myRoadTex);
		// myRoad.getRenderStates().setTiling(1);
		// myRoad.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));

		// trafficCone = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		// trafficCone.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.65f, 0.0f));
		// trafficCone.setLocalScale((new Matrix4f()).scale(0.25f, 0.25f, 0.25f));

		terrain = new GameObject(GameObject.root(), terrainShape, terrainTex);
		terrain.setIsTerrain(true);
		terrain.getRenderStates().setTiling(1);
		terrain.setLocalScale((new Matrix4f()).scale(50, 5, 50));
		terrain.setHeightMap(terrainHeightMap);
		terrain.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 0f));

		
		// terrainQ1 = new GameObject(GameObject.root(), terrainQ1S, terrainTex);
		// terrainQ1.setIsTerrain(true);
		// terrainQ1.getRenderStates().setTiling(1);
		// terrainQ1.setLocalScale((new Matrix4f()).scale(50, 4, 50));
		// terrainQ1.setHeightMap(terrainHeightMap1);
		// terrainQ1.setLocalTranslation((new Matrix4f()).translation(50f, 0f, 50f));

		// terrainQ2 = new GameObject(GameObject.root(), terrainQ2S, terrainTex);
		// terrainQ2.setIsTerrain(true);
		// terrainQ2.getRenderStates().setTiling(1);
		// terrainQ2.setLocalScale((new Matrix4f()).scale(50, 4, 50));
		// terrainQ2.setHeightMap(terrainHeightMap2);
		// terrainQ2.setLocalTranslation((new Matrix4f()).translation(-50f, 0f, 50f));
		// terrainQ2.setLocalScale((new Matrix4f()).scale(1, 1, 1));

		
		// terrainQ3 = new GameObject(GameObject.root(), terrainQ3S, terrainTex);
		// terrainQ3.setIsTerrain(true);
		// terrainQ3.getRenderStates().setTiling(1);
		// terrainQ3.setLocalScale((new Matrix4f()).scale(50, 4, 50));
		// terrainQ3.setHeightMap(terrainHeightMap3);
		// terrainQ3.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 0f));

		// terrainQ4 = new GameObject(GameObject.root(), terrainQ4S, terrainTex);
		// terrainQ4.setIsTerrain(true);
		// terrainQ4.getRenderStates().setTiling(1);
		// terrainQ4.setLocalScale((new Matrix4f()).scale(50, 4, 50));
		// terrainQ4.setHeightMap(terrainHeightMap4);
		// terrainQ4.setLocalTranslation((new Matrix4f()).translation(50f, 0f, 50f));
	}

	@Override
	public void initializeLights()
	{
		Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		engine.getSceneGraph().addLight(light);
	}

	@Override
	public void initializeGame()
	{
		setupNetworking();

		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		engine.getRenderSystem().setWindowDimensions(1900, 1000);
		engine.getRenderSystem().setLocationRelativeTo(null);

		// ----------------- initialize camera ----------------
		// positionCameraBehindAvatar();
		Camera mainCamera = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		springController = new SpringCameraController(mainCamera, avatar, engine);
		orbitController = new CameraOrbit3D(mainCamera, avatar, engine);
		initMouseMode();

		// --- initialize physics system ---
		(engine.getSceneGraph()).setPhysicsDebugEnabled(true);
		String physEngine = "tage.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0f, -5f, 0f};
		physicsEngine = PhysicsEngineFactory.createPhysicsEngine(physEngine);
		physicsEngine.initSystem();
		physicsEngine.setGravity(gravity);

		engine.getRenderSystem().setDynamicsWorld(physicsEngine.getDynamicsWorld());

		// --- create physics world ---

		float chassisMass = 1000.0f;
		float up[] = {0,1,0};
		double[] tempTransform;
		Matrix4f translation = new Matrix4f(avatar.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		float[] chassisHalfExtens = {0.6325f, 1.1525f, 0.5025f};
		avatarP = physicsEngine.addVehicleObject(physicsEngine.nextUID(), chassisMass, tempTransform, chassisHalfExtens);
		avatar.setPhysicsObject(avatarP);
		
		RaycastVehicle vehicle = physicsEngine.getVehicle();
		VehicleTuning tuning = physicsEngine.getVehicleTuning();
		
		// float wheelMass = 25.0f;
		float[] wheelHalfExtents = new float[]{0.01975f, 0.084625f, 0.0825f};
		float wheelHeight = 2 * wheelHalfExtents[1];
		float wheelWidth = 2 * wheelHalfExtents[0];

		javax.vecmath.Vector3f wheelDirectionCS0 = new javax.vecmath.Vector3f(0, -1, 0);
		javax.vecmath.Vector3f wheelAxleCS = new javax.vecmath.Vector3f(-1, 0, 0);
		float suspensionRestLength = 0.7f;

		Vector3f wheelConnectionPoint = new Vector3f(chassisHalfExtens[0] - wheelHalfExtents[0], wheelHeight , chassisHalfExtens[2] - wheelWidth);

		//Adds the front wheels
		vehicle.addWheel(toJavaxVecmath(wheelConnectionPoint), wheelDirectionCS0, wheelAxleCS, suspensionRestLength, wheelHalfExtents[0], tuning, true);
		vehicle.addWheel(toJavaxVecmath(wheelConnectionPoint.mul(new Vector3f(-1f, 1f, 1f))), wheelDirectionCS0, wheelAxleCS, suspensionRestLength, wheelHalfExtents[0], tuning, true);


		//Adds the rear wheels
		vehicle.addWheel(toJavaxVecmath(wheelConnectionPoint.mul(new Vector3f(1f, 1f, -1f))), wheelDirectionCS0, wheelAxleCS, suspensionRestLength, wheelHalfExtents[0], tuning, false);
		vehicle.addWheel(toJavaxVecmath(wheelConnectionPoint.mul(new Vector3f(-1f, 1f, -1f))), wheelDirectionCS0, wheelAxleCS, suspensionRestLength, wheelHalfExtents[0], tuning, false);

		// Edit wheel info for all 4 wheels
		for(int i = 0; i < 4; i++){
			WheelInfo wheel = vehicle.getWheelInfo(i);
			wheel.suspensionStiffness = 50;
			wheel.wheelsDampingCompression = 0.6f * Math.sqrt(wheel.suspensionStiffness);
			wheel.wheelsDampingRelaxation = Math.sqrt(wheel.suspensionStiffness);
			wheel.frictionSlip = 1.2f;
			wheel.rollInfluence = 1;
		}

		translation = new Matrix4f(terrain.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		terrainP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), tempTransform, up, 0.0f);
		// terrainP.setBounciness(1.0f);
		terrain.setPhysicsObject(terrainP);

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();
		AccelAction accelAction = new AccelAction(this, vehicle, protocolClient);
		DecelAction decelAction = new DecelAction(this, vehicle, protocolClient);
		TurnRightAction turnRightAction = new TurnRightAction(this, (float) turnConst, (float) turnCoef);
		TurnLeftAction turnLeftAction = new TurnLeftAction(this, (float) turnConst, (float) turnCoef);
		ToggleCamaraType toggleCamaraType = new ToggleCamaraType(this);
		TempRotateWheel tempRotateWheels = new TempRotateWheel(this);

		im.associateActionWithAllGamepads(Identifier.Button._1, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);


		//comment this out
		im.associateActionWithAllKeyboards(Identifier.Key.W, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, decelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Identifier.Key.D, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, turnLeftAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key._2, toggleCamaraType, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key._3, tempRotateWheels, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	public GameObject getAvatar()
	{
		return avatar;
	}

	@Override
	public void update()
	{
		Matrix4f currentTranslation, currentRotation;
		double totalTime = System.currentTimeMillis() - startTime;
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		elapsed = (float) (elapsedTime / 1000.0);
		amt = elapsedTime * 0.03;
		double amtt = totalTime * 0.001;
		

		//Temp trigger to rotate back wheels
		// if(rotatingWheels){
		// 	backLW.getPhysicsObject().applyTorque(5, 0, 0);
		// 	backRW.getPhysicsObject().applyTorque(5, 0, 0);
		// }

		
		//update physics
		if (true) {
			Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			checkForCollisions();
			physicsEngine.update((float)elapsedTime);
			for (GameObject go:engine.getSceneGraph().getGameObjects()) {
				PhysicsObject PO = go.getPhysicsObject();
				
				// Skip the code below and go to the next GameObject if the PO is null or if it's a vehicle
				if(PO == null) continue;
				
				mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
				mat2.set(3,0,mat.m30());
				mat2.set(3,1,mat.m31());
				mat2.set(3,2,mat.m32());
				go.setLocalTranslation(mat2);
			}
		}

		// build and set HUD
		String speedString = String.format("Speed: %.2f", speed);
		engine.getHUDmanager().setHUD1(speedString, new Vector3f(1, 1, 1), 15, 15);

		// My code start -- update avatar to move up with terrain
		// update altitude of dolphin based on height map
		// Vector3f loc = avatar.getWorldLocation();
		// float height = terr.getHeight(loc.x(), loc.z());
		// avatar.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		// update inputs and camera
		// stoppingForce(elapsed);
		// applyGravity(elapsed);
		im.update(elapsed);
		// positionCameraBehindAvatar();
		// updatePosition();
		processNetworking(elapsed);

		if (updateScriptInRuntime)
		{
			passes++;

			if (passes > 30)
			{
				updateScripts();
			}
		}

		if(!toggleCamaraType){
			springController.updateCameraPosition(elapsed, speed);
		} else {
			orbitController.updateCameraPosition();
		}
	}

	public void toggleCamara(){
		toggleCamaraType = !toggleCamaraType;
	}

	public void toggleRotation(){
		rotatingWheels = !rotatingWheels;
	}

	private void positionCameraBehindAvatar()
	{
		Vector4f u = new Vector4f(-1f, 0f, 0f, 1f);
		Vector4f v = new Vector4f(0f, 1f, 0f, 1f);
		Vector4f n = new Vector4f(0f, 0f, 1f, 1f);
		u.mul(avatar.getWorldRotation());
		v.mul(avatar.getWorldRotation());
		n.mul(avatar.getWorldRotation());
		Matrix4f w = avatar.getWorldTranslation();
		Vector3f position = new Vector3f(w.m30(), w.m31(), w.m32());
		position.add(-n.x() * 2f, -n.y() * 2f, -n.z() * 2f);
		position.add(v.x() * .75f, v.y() * .75f, v.z() * .75f);
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		c.setLocation(position);
		c.setU(new Vector3f(u.x(), u.y(), u.z()));
		c.setV(new Vector3f(v.x(), v.y(), v.z()));
		c.setN(new Vector3f(n.x(), n.y(), n.z()));
	}

	public double getSpeed()
	{
		return speed;
	}

	public boolean getIsFalling()
	{
		return isFalling;
	}

	public int getMaxSpeed()
	{
		return maxSpeed;
	}

	public void accelerate(float time)
	{
		if (isFalling)
		{
			return;
		}

		speed += time * acceleration;

		if (speed > maxSpeed)
		{
			speed = maxSpeed;
		}
	}

	public void decelerate(float time)
	{
		if (isFalling)
		{
			return;
		}

		speed -= time * deceleration;

		if (speed < 0)
		{
			speed = 0;
		}
	}

	private void stoppingForce(float time)
	{
		speed -= time * stoppingForce;

		if (speed < 0)
		{
			speed = 0;
		}
	}

	private void applyGravity(float time)
	{
		Vector3f pos = avatar.getWorldLocation();

		// Little trick to get the bottom of the obj to be on the ground
		pos.y += avatar.getShape().getLowestVertexY() * 0.25f;
		float floor = terrain.getHeight(pos.x, pos.z);

		if (floor < 0)
		{
			floor = 0;
		}

		if (pos.y > floor)
		{
			isFalling = true;
			gravitySpeed += time * gravity;
			pos.y -= gravitySpeed;
			if (pos.y < floor)
			{
				pos.y = floor;
				isFalling = false;
			}
		} else
		{
			isFalling = false;
			gravitySpeed = 0;
			pos.y = floor;
		}

		// Reset position back to normal
		pos.y -= avatar.getShape().getLowestVertexY() * 0.25f;
		avatar.setLocalLocation(pos);
	}

	private void updatePosition()
	{
		Vector3f oldPosition = avatar.getWorldLocation();
		Vector4f fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
		fwdDirection.mul(avatar.getWorldRotation());
		fwdDirection.mul((float) (speed * 0.1));
		Vector3f newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
		avatar.setLocalLocation(newPosition);
		protocolClient.sendMoveMessage(newPosition);
	}

	private void checkForCollisions() {
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;
	
		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
	
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++) {
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
	
			for (int j = 0; j < manifold.getNumContacts(); j++) {
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f) {
					System.out.println("---- hit between " + obj1 + " and " + obj2);
					break;
				}
			}
		}
	}
	
	// ---------- MOUSE CAMERA SECTION ------------

	/**
	 * Initializes the mouse input as a camera controller.
	 */
	private void initMouseMode()
	{
		RenderSystem rs = engine.getRenderSystem();
		Viewport vw = rs.getViewport("MAIN");
		float left = vw.getActualLeft();
		float bottom = vw.getActualBottom();
		float width = vw.getActualWidth();
		float height = vw.getActualHeight();
		centerX = (int) (left + width / 2);
		centerY = (int) (bottom - height / 2);
		mouseIsRecentering = false;

		try
		{
			robot = new Robot();
		} catch (Exception ex)
		{
			throw new RuntimeException("Couldn't create Robot!");
		}

		recenterMouse();
		prevMouseX = centerX;
		prevMouseY = centerY;

		BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "blank cursor");
		rs.getGLCanvas().setCursor(blankCursor);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		if (mouseIsRecentering && centerX == e.getXOnScreen() && centerY == e.getYOnScreen())
		{
			mouseIsRecentering = false;
		} else
		{
			curMouseX = e.getXOnScreen();
			curMouseY = e.getYOnScreen();
			double mouseDeltaX = prevMouseX - curMouseX;
			double mouseDeltaY = prevMouseY - curMouseY;

			if(!toggleCamaraType){
				springController.mouseMove((float) mouseDeltaX, (float) mouseDeltaY);
			} else {
				orbitController.mouseMove((float) mouseDeltaX, (float) mouseDeltaY);
			}

			recenterMouse();
			prevMouseX = centerX; // reset prev to center
			prevMouseY = centerY;
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		orbitController.mouseZoom(-e.getWheelRotation());
	}

	/**
	 * Recenters the mouse.
	 */
	private void recenterMouse()
	{
		mouseIsRecentering = true;
		robot.mouseMove((int) centerX, (int) centerY);
	}

	// ---------- SCRIPTING SECTION ----------------

	/**
	 * Read and evaluate JS expressions into our JS engine
	 * 
	 * @param scriptFile File for JS scripts
	 * @author Scott V. Gordon
	 */
	private void runScript(File scriptFile)
	{
		try
		{
			FileReader fileReader = new FileReader(scriptFile);
			jsEngine.eval(fileReader);
			fileReader.close();
		} catch (FileNotFoundException e1)
		{
			System.out.println(scriptFile + " not found " + e1);
		} catch (IOException e2)
		{
			System.out.println("IO problem with " + scriptFile + e2);
		} catch (ScriptException e3)
		{
			System.out.println("ScriptException in " + scriptFile + e3);
		} catch (NullPointerException e4)
		{
			System.out.println("Null ptr exception reading " + scriptFile + e4);
		}
	}

	/**
	 * Run the scripts and update the contstants for the game. This can be run
	 * during update if you want the values to be edited during runtime.
	 * <p>
	 * In order to update during runtime, 'updateDuringRuntime' must be set to true
	 * in the JS script
	 */
	private void updateScripts()
	{
		Log.trace("Updating script");
		runScript(scriptFile);
		maxSpeed = (Integer) jsEngine.get("maxSpeed");
		acceleration = (Double) jsEngine.get("acceleration");
		stoppingForce = (Double) jsEngine.get("stoppingForce");
		gravity = (Double) jsEngine.get("gravity");
		deceleration = (Double) jsEngine.get("deceleration");
		turnConst = (Double) jsEngine.get("turnConst");
		turnCoef = (Double) jsEngine.get("turnCoef");
		updateScriptInRuntime = (Boolean) jsEngine.get("updateDuringRuntime");
		if (updateScriptInRuntime)
		{
			if ((Boolean) jsEngine.get("allowLogLevelChange"))
			{
				Log.setLogLevel((Integer) jsEngine.get("logLevel"));
			}
		}
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape()
	{
		return ghostShape;
	}

	public TextureImage getGhostTexture()
	{
		return ghostTex;
	}

	public GhostManager getGhostManager()
	{
		return ghostManager;
	}

	public Engine getEngine()
	{
		return engine;
	}

	private void setupNetworking()
	{
		isClientConnected = false;
		try
		{
			protocolClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} catch (UnknownHostException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		if (protocolClient == null)
		{
			System.out.println("missing protocol host");
		} else
		{ // Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protocolClient.sendJoinMessage();
		}
	}

	protected void processNetworking(float elapsTime)
	{ // Process packets received by the client from the server
		if (protocolClient != null)
			protocolClient.processPackets();
	}

	public Vector3f getPlayerPosition()
	{
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value)
	{
		this.isClientConnected = value;
	}

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		@Override
		public void performAction(float time, net.java.games.input.Event evt)
		{
			if (protocolClient != null && isClientConnected == true)
			{
				protocolClient.sendByeMessage();
			}
		}
	}

	private class ToggleCamaraType extends AbstractInputAction {
		MyGame myGame;

		ToggleCamaraType(MyGame myGame){
			this.myGame = myGame;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt)
		{
			myGame.toggleCamara();
		}
	}

	private class TempRotateWheel extends AbstractInputAction {
		MyGame myGame;

		TempRotateWheel(MyGame myGame){
			this.myGame = myGame;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt)
		{
			myGame.toggleRotation();
		}
	}


// ------------------ UTILITY FUNCTIONS used by physics
public static javax.vecmath.Vector3f toJavaxVecmath(Vector3f jomlVec) {
    return new javax.vecmath.Vector3f(jomlVec.x, jomlVec.y, jomlVec.z);
}

private float[] toFloatArray(double[] arr) {
    if (arr == null) {
        return null;
    }
    int n = arr.length;
    float[] ret = new float[n];
    for (int i = 0; i < n; i++) {
        ret[i] = (float)arr[i];
    }
    return ret;
}

private double[] toDoubleArray(float[] arr) {
    if (arr == null) {
        return null;
    }
    int n = arr.length;
    double[] ret = new double[n];
    for (int i = 0; i < n; i++) {
        ret[i] = (double)arr[i];
    }
    return ret;
}

}
