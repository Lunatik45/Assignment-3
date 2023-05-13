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
import java.util.ArrayList;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;

import net.java.games.input.Event;
import net.java.games.input.Component.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.AxisAngle4f;

import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.jogamp.opengl.math.Matrix4;

import tage.audio.AudioManagerFactory;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;
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
import tage.shapes.AnimatedShape;
import tage.shapes.ImportedModel;
import tage.shapes.TerrainPlane;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import tage.physics.PhysicsHingeConstraint;

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

	private AudioResource engineResource, bgMusicResource;
	private Camera mainCamera;
	private CameraOrbit3D orbitController;
	private SpringCameraController springController;
	private File scriptFile;
	private ArrayList<GameObject> stationary, dynamic;
	private GameObject avatar, terrain, terrainQ1, terrainQ2, terrainQ3, terrainQ4, trafficCone, myRoad, frontRW,
			frontLW, backRW, backLW, waypoint, tcBarrier1, tcBarrier2, tcBarrier3, tcBarrier4;
	private AnimatedShape avatarAS;
	private GhostManager ghostManager;
	private IAudioManager audioMgr;
	private InputManager im;
	private Light light;
	private ObjShape ghostShape, dolphinShape, terrainShape, terrainQ1S, terrainQ2S, terrainQ3S, terrainQ4S,
			trafficConeShape, boxCarShape, myRoadShape, frontRWShape, frontLWShape, backRWShape, backLWShape,
			building1Shape, building2Shape, building3Shape, building4Shape, trafficB3Shape, trafficB2Shape,
			trafficB1Shape, arrowShape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private Robot robot;
	private ScriptEngine jsEngine;
	private Sound engineSound, bgSound;
	private ArrayList<Sound> ghostSounds;
	private String serverAddress;
	private TextureImage dolphinTex, ghostTex, terrainTex, trafficConeTex, boxCarTex, myRoadTex, avatarTex,
			greenAvatarTex, redAvatarTex, blueAvatarTex, whiteAvatarTex, terrainHeightMap, building1Tex, building2Tex,
			building3Tex, building4Tex, trafficTex, arrowTex;
	private TextureImage terrainHeightMap1, terrainHeightMap2, terrainHeightMap3, terrainHeightMap4;
	private NpcManager npcManager;
	private RaycastVehicle vehicle, npcVehicle;
	private ArrayList<Vector2f> targets;
	private Vector2f targetPos;

	private boolean isClientConnected = false, isNpcHandler = false, race = false, racePrep = false, raceDone = false;
	private float vals[] = new float[16];
	private boolean isFalling = false, updateScriptInRuntime, newTarget = true;
	private double centerX, centerY, prevMouseX, prevMouseY, curMouseX, curMouseY;
	private double acceleration, deceleration, stoppingForce, gravity, speed = 0, gravitySpeed = 0, turnConst, turnCoef;
	private double startTime, prevTime, elapsedTime, amt, volume = 1, totalTime;
	private float elapsed, targetMargin = 2;
	private int maxVolBG = 40, maxVolEng = 80, lakeIslands, maxSpeed, passes = 0, target = 0;
	private int serverPort, avatarPhysicsUID, npcPhysicsUID;
	private PhysicsEngine physicsEngine;
	private PhysicsObject avatarP, trafficConeP, terrainP, frontRWP, frontLWP, backRWP, backLWP, npcP, building2P;
	private PhysicsHingeConstraint frontRWHinge, frontLWHinge, backRWHinge, backLWHinge;
	private Boolean toggleCameraType = false;
	private Boolean toggleAnimation = false;
	private Boolean mouseIsRecentering = false;
	private String textureSelection = "";

	public MyGame(String serverAddress, int serverPort, String protocol, int debug)
	{
		super();

		Log.setLogLevel(debug);
		ghostManager = new GhostManager(this);
		npcManager = new NpcManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;

		if (protocol.toUpperCase().compareTo("UDP") == 0)
			this.serverProtocol = ProtocolType.UDP;
		else
			this.serverProtocol = null;

		ScriptEngineManager factory = new ScriptEngineManager();
		jsEngine = factory.getEngineByName("js");
		scriptFile = new File("assets/scripts/params.js");
		updateScripts();

		if (updateScriptInRuntime)
		{
			Log.print("CAUTION: Script will update during runtime.\n");
			Log.print("CAUTION: Performance may be affected while this mode is in use.\n");
		}

		selectCar();
	}

	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e)
		{
			e.printStackTrace();
		}

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

	/**
	 * Allows car selection using a JOptionDialog.
	 */
	private void selectCar()
	{
		JRadioButton greenOption = new JRadioButton("Green");
		JRadioButton blueOption = new JRadioButton("Blue");
		JRadioButton redOption = new JRadioButton("Red");
		JRadioButton whiteOption = new JRadioButton("White");

		// Make it so that only one option can be selected at a time
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(greenOption);
		buttonGroup.add(blueOption);
		buttonGroup.add(redOption);
		buttonGroup.add(whiteOption);

		JPanel selectionPanel = new JPanel();
		selectionPanel.add(new JLabel("Select car type:  "));
		selectionPanel.add(greenOption);
		selectionPanel.add(blueOption);
		selectionPanel.add(redOption);
		selectionPanel.add(whiteOption);

		int result = JOptionPane.showOptionDialog(null, selectionPanel, "Car Selection", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null);

		if (result == JOptionPane.OK_OPTION)
		{
			if (greenOption.isSelected())
			{
				textureSelection = "CarTexture.png";
			} else if (blueOption.isSelected())
			{
				textureSelection = "CarTextureBlue.png";
			} else if (redOption.isSelected())
			{
				textureSelection = "CarTextureRed.png";
			} else if (whiteOption.isSelected())
			{
				textureSelection = "CarTextureWhite.png";
			}
		}

		if (textureSelection.length() == 0)
		{
			Log.print("No selection. Choosing default.\n");
			textureSelection = "CarTexture.png";
		}

		Log.trace("Selection: %s\n", textureSelection);
	}

	@Override
	public void loadShapes()
	{
		avatarAS = new AnimatedShape("car.rkm", "car.rks");
		avatarAS.loadAnimation("ACCEL", "car.rka");
		ghostShape = new ImportedModel("box_car.obj");
		dolphinShape = new ImportedModel("dolphinHighPoly.obj");
		trafficConeShape = new ImportedModel("trafficCone.obj");
		terrainShape = new TerrainPlane(100);
		// terrainQ1S = new TerrainPlane(25);
		// terrainQ2S = new TerrainPlane(25);
		// terrainQ3S = new TerrainPlane(25);
		// terrainQ4S = new TerrainPlane(25);
		myRoadShape = new ImportedModel("myRoad.obj");
		// boxCarShape = new ImportedModel("box_car.obj");
		building1Shape = new ImportedModel("Building1.obj");
		building2Shape = new ImportedModel("Building2.obj");
		building3Shape = new ImportedModel("Building3.obj");
		building4Shape = new ImportedModel("Building4.obj");
		trafficB3Shape = new ImportedModel("TrafficBarricade3.obj");
		trafficB2Shape = new ImportedModel("TrafficBarricade2.obj");
		trafficB1Shape = new ImportedModel("TrafficBarricade1.obj");
		arrowShape = new ImportedModel("arrow.obj");
	}

	@Override
	public void loadTextures()
	{
		dolphinTex = new TextureImage("Dolphin_HighPolyUV.png");
		trafficConeTex = new TextureImage("traffic_cone.png");
		ghostTex = new TextureImage("CarTexture.png");
		terrainTex = new TextureImage("tileable_grass_01.png");
		// terrainTex = new TextureImage("HMT.jpg");
		terrainHeightMap = new TextureImage("terrain1.jpg");

		// Used for testing, unless wanted can be deleted
		terrainHeightMap1 = new TextureImage("terrain1_1.jpg");
		terrainHeightMap2 = new TextureImage("terrain1_2.jpg");
		terrainHeightMap3 = new TextureImage("terrain1_3.jpg");
		terrainHeightMap4 = new TextureImage("terrain1_4.jpg");

		boxCarTex = new TextureImage("CarTexture.png");
		myRoadTex = new TextureImage("road1.jpg");
		// terrainHeightMap = new TextureImage("HM1.jpg");
		greenAvatarTex = new TextureImage("CarTexture.png");
		blueAvatarTex = new TextureImage("CarTextureBlue.png");
		redAvatarTex = new TextureImage("CarTextureRed.png");
		whiteAvatarTex = new TextureImage("CarTextureWhite.png");

		avatarTex = getAvatarTex(textureSelection);

		building1Tex = new TextureImage("Building1.jpg");
		building2Tex = new TextureImage("Building2.jpg");
		building3Tex = new TextureImage("Building3.jpg");
		building4Tex = new TextureImage("Building4.jpg");

		trafficTex = new TextureImage("Traffic.jpg");

		arrowTex = new TextureImage("arrow.png");
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
		stationary = new ArrayList<GameObject>();
		dynamic = new ArrayList<GameObject>();
		GameObject newObj;

		// terrain = new GameObject(GameObject.root(), terrainShape, terrainTex);
		// terrain.setIsTerrain(true);
		// terrain.getRenderStates().setTiling(1);
		// terrain.setLocalScale((new Matrix4f()).scale(50, 5, 50));
		// terrain.setLocalTranslation((new Matrix4f()).translateLocal(0, 0, 0));
		// // terrain.getRenderStates().setWireframe(true);
		// terrain.setHeightMap(terrainHeightMap);

		float heightOffGround = -avatarAS.getLowestVertexY();
		avatar = new GameObject(GameObject.root(), avatarAS, avatarTex);
		avatar.setLocalScale((new Matrix4f()).scale(.25f, .25f, .25f));
		avatar.setLocalTranslation((new Matrix4f()).translate(0.0f, heightOffGround, 0.0f));

		// Template:
		// newObj = new GameObject(GameObject.root(), shape, tex);
		// newObj.setLocalScale((new Matrix4f()).scale(1f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		// stationary.add(newObj);

		// Add object primarily meant to be stationary
		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(18f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-4.0f, 0.0f, 0.0f));
		// newObj.getRenderStates().setWireframe(true);
		stationary.add(newObj);

		// Only stationary obj with a physics object aside from the map boundaries
		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(0f, 0f, 0f));
		// newObj.getRenderStates().setWireframe(true);
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building3Shape, building3Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.06f));
		newObj.setLocalTranslation((new Matrix4f()).translate(4.0f, 0.0f, 0.0f));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building4Shape, building4Tex);
		newObj.setLocalScale((new Matrix4f()).scale(3.0f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-5.0f, 0.0f, 15.0f));
		stationary.add(newObj);

		// Add objects that have potential to be dynamic (physics)
		// newObj = new GameObject(GameObject.root(), trafficB3Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		// dynamic.add(newObj);

		// newObj = new GameObject(GameObject.root(), trafficB2Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		// dynamic.add(newObj);

		// newObj = new GameObject(GameObject.root(), trafficB1Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		// dynamic.add(newObj);

		// newObj = new GameObject(GameObject.root(), trafficB3Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(25f, 0.0f, 25f));
		// dynamic.add(newObj);

		// newObj = new GameObject(GameObject.root(), trafficB3Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(-25f, 0.0f, 25f));
		// dynamic.add(newObj);

		// newObj = new GameObject(GameObject.root(), trafficB3Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(-25f, 0.0f, -25f));
		// dynamic.add(newObj);

		// newObj = new GameObject(GameObject.root(), trafficB3Shape, trafficTex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(25f, 0.0f, -25f));
		// dynamic.add(newObj);

		waypoint = new GameObject(GameObject.root(), arrowShape, arrowTex);
		waypoint.setLocalScale((new Matrix4f()).scale(1.25f));


		// backRW = new GameObject(avatar, backRWShape, boxCarTex);
		// backLW = new GameObject(avatar, backLWShape, boxCarTex);
		// frontRW = new GameObject(avatar, frontRWShape, boxCarTex);
		// frontLW = new GameObject(avatar, frontLWShape, boxCarTex);

		myRoad = new GameObject(GameObject.root(), myRoadShape, myRoadTex);
		myRoad.getRenderStates().setTiling(1);
		// myRoad.getRenderStates().setWireframe(true);
		myRoad.setLocalTranslation((new Matrix4f()).translate(0.0f, 0f, 0.0f));

		// trafficCone = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		// trafficCone.setLocalTranslation((new Matrix4f()).translate(1.0f, 0.215f, 1.0f));


		// Just applying the barriers to random/hidden game object so that it can be checked for collisions with our algo
		tcBarrier1 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier1.setLocalScale((new Matrix4f()).scale(0.01f));
		tcBarrier1.setLocalTranslation((new Matrix4f()).translate(0f, -100.0f, 0f));

		tcBarrier2 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier2.setLocalScale((new Matrix4f()).scale(0.01f));
		tcBarrier2.setLocalTranslation((new Matrix4f()).translate(0f, -100.0f, 0f));

		tcBarrier3 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier3.setLocalScale((new Matrix4f()).scale(0.01f));
		tcBarrier3.setLocalTranslation((new Matrix4f()).translate(0f, -100.0f, 0f));

		tcBarrier4 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier4.setLocalScale((new Matrix4f()).scale(0.01f));
		tcBarrier4.setLocalTranslation((new Matrix4f()).translate(0f, -100.0f, 0f));

		// terrain = new GameObject(GameObject.root(), terrainShape);
		// terrain.getRenderStates().setWireframe(true);

		// terrain.setIsTerrain(true);
		// terrain.getRenderStates().setTiling(1);
		// terrain.setLocalScale((new Matrix4f()).scale(50, 5, 50));
		// terrain.setHeightMap(terrainHeightMap);
		// terrain.setLocalTranslation((new Matrix4f()).translation(0f, -2f, 0f));


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
		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		engine.getRenderSystem().setWindowDimensions(1900, 1000);
		engine.getRenderSystem().setLocationRelativeTo(null);
		mainCamera = (engine.getRenderSystem().getViewport("MAIN").getCamera());

		setupTargets();

		setupNetworking();

		setupSounds();

		// ----------------- initialize camera ----------------
		// positionCameraBehindAvatar();
		Camera mainCamera = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		springController = new SpringCameraController(mainCamera, avatar, engine);
		orbitController = new CameraOrbit3D(mainCamera, avatar, engine);
		initMouseMode();

		// --- initialize physics system ---
		String physEngine = "tage.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = { 0f, -9.8f, 0f };
		physicsEngine = PhysicsEngineFactory.createPhysicsEngine(physEngine);
		physicsEngine.initSystem();
		physicsEngine.setGravity(gravity);

		// Used to see boxShape
		// (engine.getSceneGraph()).setPhysicsDebugEnabled(true);
		// engine.getRenderSystem().setDynamicsWorld(physicsEngine.getDynamicsWorld());

		// --- create physics world ---

		float chassisMass = 1500.0f;
		float up[] = { 0, 1, 0 };
		double[] tempTransform, npcTransform;
		Matrix4f translation = new Matrix4f(avatar.getWorldTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		// translation = new Matrix4f().translation(5, 1, 5);
		// npcTransform = toDoubleArray(translation.get(vals));
		// float[] chassisHalfExtens = {0.316f, 0.251f, 0.575f};
		float[] chassisHalfExtens = { 1f, 0.5f, 2f };
		avatarPhysicsUID = physicsEngine.nextUID();
		// npcPhysicsUID = physicsEngine.nextUID();
		avatarP = physicsEngine.addVehicleObject(avatarPhysicsUID, chassisMass, tempTransform, chassisHalfExtens);
		// npcP = physicsEngine.addVehicleObject(npcPhysicsUID, chassisMass,
		// npcTransform, chassisHalfExtens);

		avatar.setPhysicsObject(avatarP);

		vehicle = physicsEngine.getVehicle(avatarP.getUID());
		VehicleTuning tuning = physicsEngine.getVehicleTuning(avatarP.getUID());

		float wheelRadius = 0.5f;
		float connectionHeight = 1.2f;
		float wheelWidth = 0.4f;

		physicsEngine.addWheels(vehicle, tuning, chassisHalfExtens, wheelRadius, connectionHeight, wheelWidth);

		// Crude implimentation for NPC physics
		// npcVehicle = physicsEngine.getVehicle(npcP.getUID());
		// VehicleTuning npcTuning = physicsEngine.getVehicleTuning(npcP.getUID());

		// physicsEngine.addWheels(npcVehicle, npcTuning, chassisHalfExtens,
		// wheelRadius, connectionHeight, wheelWidth);

		translation = new Matrix4f(myRoad.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		// terrainP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(),
		// tempTransform, up, 0.0f);
		// terrainP.setFriction(1.0f);

		translation = new Matrix4f();
		tempTransform = toDoubleArray(translation.get(vals));
		float [] planeSize = {2000, 1f , 2000f};
		com.bulletphysics.linearmath.Transform moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(0, -1, 0);
		terrainP = physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform, planeSize);
		myRoad.setPhysicsObject(terrainP);
		terrainP.setFriction(0.25f);

		// Traffic cone phys obj
		// translation = new Matrix4f(trafficCone.getLocalTranslation());
		// tempTransform = toDoubleArray(translation.get(vals));
		// float [] coneSize = {0.120f, 0.184f , 0.120f};
		// trafficConeP = physicsEngine.addBoxObject(physicsEngine.nextUID(), 5f,
		// tempTransform, coneSize);
		// trafficCone.setPhysicsObject(trafficConeP);
		// trafficConeP.setBounciness(0.4f);
		// initMouseMode();

		
		// Setting up map barriers
		translation = new Matrix4f();
		tempTransform = toDoubleArray(translation.get(vals));
		float[] barrierSize1 = { 2000f, 40.0f, 1.0f }; // wide along the x axis, placement altered along z axis
		float[] barrierSize2 = { 1f, 40.0f, 1998.0f }; // wide along the z axis, placement altered along x axis
		moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(999f, 10f, 0f);
		tcBarrier1.setPhysicsObject(physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform,
		barrierSize2, moveTo));

		translation = new Matrix4f();
		tempTransform = toDoubleArray(translation.get(vals));
		moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(1f, 20.0f, -999f);
		tcBarrier2.setPhysicsObject(physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform,
		barrierSize1, moveTo));

		translation = new Matrix4f();
		tempTransform = toDoubleArray(translation.get(vals));
		moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(-999.0f, 20.0f, 1.0f);
		tcBarrier3.setPhysicsObject(physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform,
		barrierSize2, moveTo));

		translation = new Matrix4f();
		tempTransform = toDoubleArray(translation.get(vals));
		moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(1f, 20.0f, 999f);
		tcBarrier4.setPhysicsObject(physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform,
		barrierSize1, moveTo));

		// Building 2 phys obj 
		// GameObject tempObj = stationary.get(1); //building2
		// translation = new Matrix4f();
		// tempTransform = toDoubleArray(translation.get(vals));
		// float[] barrier1Size = { 120.0f, 110.0f, 60.0f };
		// moveTo = new com.bulletphysics.linearmath.Transform();
		// moveTo.setIdentity();
		// moveTo.origin.set(-32.4f, 55f, 54.803f);
		// building2P =  physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform,
		// 		barrier1Size, moveTo);
		// tempObj.setPhysicsObject(building2P);

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();
		AccelAction accelAction = new AccelAction(this, vehicle, protocolClient);
		DecelAction decelAction = new DecelAction(this, vehicle, protocolClient);
		TurnRightAction turnRightAction = new TurnRightAction(this, (float) turnConst, (float) turnCoef, vehicle);
		TurnLeftAction turnLeftAction = new TurnLeftAction(this, (float) turnConst, (float) turnCoef, vehicle);
		ToggleCameraType toggleCameraType = new ToggleCameraType(this);
		IncreaseVolume increaseVolume = new IncreaseVolume();
		DecreaseVolume decreaseVolume = new DecreaseVolume();
		ToggleAnimationType toggleAnimationType = new ToggleAnimationType(this);
		PrepRaceAction prepRaceAction = new PrepRaceAction();

		im.associateActionWithAllGamepads(Identifier.Button._1, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);


		im.associateActionWithAllKeyboards(Identifier.Key.W, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, decelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, turnLeftAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.O, increaseVolume, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key.L, decreaseVolume, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key._2, toggleCameraType, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key._3, toggleAnimationType, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key.R, prepRaceAction, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	@Override
	public void shutdown()
	{
		super.shutdown();

		ghostManager.shutdown();
		npcManager.shutdown();

		engineSound.release(audioMgr);
		bgSound.release(audioMgr);
		engineResource.unload();
		bgMusicResource.unload();
		audioMgr.shutdown();

		sendByeMessage();
	}

	@Override
	public void update()
	{
		// Matrix4f currentTranslation, currentRotation;
		// double totalTime = System.currentTimeMillis() - startTime;
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		elapsed = (float) (elapsedTime / 1000.0);
		amt = elapsedTime * 0.03;

		com.bulletphysics.linearmath.Transform printTransform = new com.bulletphysics.linearmath.Transform();
		vehicle.getChassisWorldTransform(printTransform);
		
		// Turn off
		// System.out.println(vehicle.getCurrentSpeedKmHour());
		if (vehicle.getCurrentSpeedKmHour() > 0f && !toggleAnimation)
		{
			toggleAnimation = !toggleAnimation;
			avatarAS.stopAnimation();
			avatarAS.playAnimation("ACCEL", 0.5f, AnimatedShape.EndType.LOOP, 0);
		} else if (vehicle.getCurrentSpeedKmHour() < 0f && toggleAnimation)
		{
			toggleAnimation = !toggleAnimation;
			avatarAS.stopAnimation();
		} else
		{
			avatarAS.updateAnimation();
		}

		// update physics
		Matrix4f mat = new Matrix4f();
		Matrix4f mat2 = new Matrix4f().identity();
		// checkForCollisions();
		checkForNpcCollision();
		physicsEngine.update((float) elapsedTime);
		for (GameObject go : engine.getSceneGraph().getGameObjects())
		{
			PhysicsObject po = go.getPhysicsObject();

			// Skip the code below and go to the next GameObject if the PO is null or if it's a static object
			if (po == null || !po.isDynamic())
				continue;

			mat.set(toFloatArray(po.getTransform()));
			mat2.set(3, 0, mat.m30());
			mat2.set(3, 1, mat.m31());
			mat2.set(3, 2, mat.m32());
			go.setLocalTranslation(mat2);

			AxisAngle4f aa = new AxisAngle4f();
			mat.getRotation(aa);

			Matrix4f rotMatrix4f = new Matrix4f();
			rotMatrix4f.rotation(aa);

			go.setLocalRotation(rotMatrix4f);
		}

		for (int i = 0; i < 4; i++)
		{
			vehicle.setSteeringValue(vehicle.getSteeringValue(i) * 0.90f, i);
			vehicle.setBrake(0, i);
			vehicle.applyEngineForce(0, i);
		}

		// build and set HUD
		speed = vehicle.getCurrentSpeedKmHour();
		speed = speed < 1 ? 0 : speed;
		Vector3f pos = avatar.getWorldLocation();
		String hud = String.format("Speed: %.2f", speed);
		String p = String.format("x: %6.2f   y: %6.2f,   z: %6.2f", pos.x, pos.y, pos.z);
		engine.getHUDmanager().setHUD1(hud, new Vector3f(1, 1, 1), 15, 15);
		engine.getHUDmanager().setHUD2(p, new Vector3f(1, 1, 1), 200, 15);

		if (newTarget)
		{
			if (target >= targets.size())
			{
				targetPos = new Vector2f(0, 0);
				protocolClient.sendFinishedRaceMessage();
				raceDone = true;
			}
			else
			{
				targetPos = targets.get(target);
			}
			waypoint.setLocalLocation(new Vector3f(targetPos.x, 2.0f, targetPos.y));
			newTarget = false;
		}

		waypoint.worldYaw(0.03f);

		if (!racePrep)
		{
			im.update(elapsed);
		}

		if (race)
		{
			updateNpc(elapsed);
		}

		updatePosition();
		processNetworking(elapsed);

		if (updateScriptInRuntime)
		{
			if (++passes > 30)
			{
				updateScripts();
				passes = 0;
			}
		}

		if (!toggleCameraType)
		{
			springController.updateCameraPosition(elapsed, speed);
		} else
		{
			orbitController.updateCameraPosition();
		}
		updateSounds();
	}

	public void toggleCamera()
	{
		toggleCameraType = !toggleCameraType;
	}

	public void ToggleAnimation()
	{
		toggleAnimation = !toggleAnimation;
		if (toggleAnimation)
		{
			avatarAS.stopAnimation();
			avatarAS.playAnimation("ACCEL", 0.5f, AnimatedShape.EndType.LOOP, 0);
		} else
		{
			avatarAS.stopAnimation();
		}
	}

	// --------- Target Section --------
	private void setupTargets()
	{
		targets = new ArrayList<>();
		targets.add(new Vector2f(-53.5f, 2.5f));
		targets.add(new Vector2f(-200.5f, 3.5f));
		// targets.add(new Vector2f(-349f, 8.5f));
		// targets.add(new Vector2f(-545f, 102f));
	}

	// --------- Audio Section --------
	private void setupSounds()
	{
		audioMgr = AudioManagerFactory.createAudioManager("tage.audio.joal.JOALAudioManager");
		if (!audioMgr.initialize())
		{
			System.out.println("Audio Manager failed to initialize");
			return;
		}

		bgMusicResource = audioMgr.createAudioResource("assets/sounds/Lobo Loco - Fietschie Quietschie (ID 1927).wav",
				AudioResourceType.AUDIO_STREAM);
		bgSound = new Sound(bgMusicResource, SoundType.SOUND_MUSIC, 100, true);
		bgSound.initialize(audioMgr);
		bgSound.setRollOff(0.0f);
		bgSound.play(getBgVolume(), true);

		engineResource = audioMgr.createAudioResource("assets/sounds/engine-6000.wav", AudioResourceType.AUDIO_SAMPLE);
		engineSound = new Sound(engineResource, SoundType.SOUND_EFFECT, 100, true);
		engineSound.initialize(audioMgr);
		engineSound.setMaxDistance(50.0f);
		engineSound.setMinDistance(3.0f);
		engineSound.setRollOff(2.0f);
		engineSound.setLocation(getPlayerPosition());
		engineSound.play(getEngVolume(), true);

		updateEar();
	}

	private void updateSounds()
	{
		updateEar();
		engineSound.setLocation(getPlayerPosition());
		npcManager.updateSounds();

		// Ghost manager updates sound every server update
	}

	public Sound getNewEngineSound()
	{
		return new Sound(engineResource, SoundType.SOUND_EFFECT, 100, true);
	}

	public IAudioManager getAudioManager()
	{
		return audioMgr;
	}

	private void updateEar()
	{
		audioMgr.getEar().setLocation(mainCamera.getLocation());
		audioMgr.getEar().setOrientation(mainCamera.getN(), mainCamera.getV());
	}

	public int getBgVolume()
	{
		return (int) (maxVolBG * volume);
	}

	public int getEngVolume()
	{
		return (int) (maxVolEng * volume);
	}

	public void updateVolume()
	{
		npcManager.updateVolume();
		ghostManager.updateVolume();
	}

	// --------- NPC Section --------

	private void updateNpc(float time)
	{
		if (!isNpcHandler || npcManager.getNpc() == null || !isClientConnected)
		{
			return;
		}

		NpcAvatar npc = npcManager.getNpc();
		PhysicsObject po = npc.getPhysicsObject();
		RaycastVehicle v = physicsEngine.getVehicle(po.getUID());

		if (npc.wantsAccel)
		{
			v.applyEngineForce(4000, 2);
			v.applyEngineForce(4000, 3);
			v.setBrake(0, 2);
			v.setBrake(0, 3);
		} else if (npc.wantsDecel)
		{
			v.setBrake(100, 2);
			v.setBrake(100, 3);
		} else
		{
			v.applyEngineForce(0, 2);
			v.applyEngineForce(0, 3);
			v.setBrake(0, 2);
			v.setBrake(0, 3);
		}

		if ((npc.wantsTurnLeft || npc.wantsTurnRight) && v.getCurrentSpeedKmHour() < 5)
		{
			v.applyEngineForce(600, 2);
			v.applyEngineForce(600, 3);
			v.setBrake(0, 2);
			v.setBrake(0, 3);
		}

		if (npc.wantsTurnLeft)
		{
			v.setSteeringValue(0.5f, 0);
			v.setSteeringValue(0.5f, 1);
			v.setSteeringValue(-0.25f, 2);
			v.setSteeringValue(-0.25f, 3);
		} else if (npc.wantsTurnRight)
		{
			v.setSteeringValue(-0.5f, 0);
			v.setSteeringValue(-0.5f, 1);
			v.setSteeringValue(0.25f, 2);
			v.setSteeringValue(0.25f, 3);
		} else
		{
			v.setSteeringValue(0, 0);
			v.setSteeringValue(0, 1);
			v.setSteeringValue(0, 2);
			v.setSteeringValue(0, 3);
		}

		protocolClient.sendNpcMoveMessage(npc.getWorldLocation(), getLookAt(npc), 1);
	}

	public void setPrimaryNpcHandler()
	{
		isNpcHandler = true;
	}

	public NpcManager getNpcManager()
	{
		return npcManager;
	}

	public PhysicsObject getNpcPhysicsObject()
	{
		float chassisMass = 1500.0f;
		float wheelRadius = 0.5f;
		float connectionHeight = 1.2f;
		float wheelWidth = 0.4f;
		Matrix4f translation = new Matrix4f().translation(5, 1, 5);
		double[] npcTransform = toDoubleArray(translation.get(vals));
		float[] chassisHalfExtens = { 1f, 0.5f, 2f };
		npcPhysicsUID = physicsEngine.nextUID();
		npcP = physicsEngine.addVehicleObject(npcPhysicsUID, chassisMass, npcTransform, chassisHalfExtens);
		npcVehicle = physicsEngine.getVehicle(npcP.getUID());
		VehicleTuning npcTuning = physicsEngine.getVehicleTuning(npcP.getUID());

		physicsEngine.addWheels(npcVehicle, npcTuning, chassisHalfExtens, wheelRadius, connectionHeight, wheelWidth);

		return npcP;
	}

	// --------- Movement Section --------

	private void updatePosition()
	{
		if (!isClientConnected)
		{
			return;
		}

		Vector3f pos = avatar.getWorldLocation();

		if (race && !raceDone)
		{
			Vector2f pos2f = new Vector2f(pos.x, pos.z);
			if (pos2f.distance(targetPos) < targetMargin)
			{
				target++;
				newTarget = true;
			}
		}

		protocolClient.sendMoveMessage(pos, getLookAt(avatar), engineSound.getPitch());
	}

	private void checkForCollisions()
	{
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld = ((JBulletPhysicsEngine) physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();

		int manifoldCount = dispatcher.getNumManifolds();
		for (int i = 0; i < manifoldCount; i++)
		{
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

			for (int j = 0; j < manifold.getNumContacts(); j++)
			{
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f)
				{


					// if ((obj1.getUID() == avatarPhysicsUID && obj2.getUID() == npcPhysicsUID)
					// || (obj1.getUID() == npcPhysicsUID && obj2.getUID() == avatarPhysicsUID))
					// {
					// System.out.println("---- hit between avatar and npc");
					// protocolClient.forceNpcUpdate(npcManager.getNpc().getWorldLocation(),
					// getLookAt(npcManager.getNpc()), 1);
					// }

					break;
				}
			}
		}
	}

	private void checkForNpcCollision()
	{
		if (!isNpcHandler && npcManager.getNpc() != null)
		{
			com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
			com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
			com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
			com.bulletphysics.dynamics.RigidBody object1, object2;
			com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

			dynamicsWorld = ((JBulletPhysicsEngine) physicsEngine).getDynamicsWorld();
			dispatcher = dynamicsWorld.getDispatcher();

			int manifoldCount = dispatcher.getNumManifolds();
			for (int i = 0; i < manifoldCount; i++)
			{
				manifold = dispatcher.getManifoldByIndexInternal(i);
				object1 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody0();
				object2 = (com.bulletphysics.dynamics.RigidBody) manifold.getBody1();
				JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
				JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
				if ((obj1.getUID() == avatarPhysicsUID && obj2.getUID() == npcPhysicsUID)
						|| (obj1.getUID() == npcPhysicsUID && obj2.getUID() == avatarPhysicsUID))
				{
					for (int j = 0; j < manifold.getNumContacts(); j++)
					{
						contactPoint = manifold.getContactPoint(j);
						if (contactPoint.getDistance() < 0.0f)
						{

							System.out.println("---- hit between avatar and npc ----");
							protocolClient.forceNpcUpdate(npcManager.getNpc().getWorldLocation(),
									getLookAt(npcManager.getNpc()), 1);
							break;
						}
					}
				}
			}
		}
	}

	// ---------- MOUSE CAMERA SECTION ------------

	public void toggleCamara()
	{
		toggleCameraType = !toggleCameraType;
	}

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

			if (!toggleCameraType)
			{
				springController.mouseMove((float) mouseDeltaX, (float) mouseDeltaY);
			} else
			{
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
		Log.trace("Updating script\n");
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

	public TextureImage getAvatarTex(String selection)
	{
		if (greenAvatarTex.getTextureFile().contains(selection))
		{
			return greenAvatarTex;
		} else if (redAvatarTex.getTextureFile().contains(selection))
		{
			return redAvatarTex;
		} else if (blueAvatarTex.getTextureFile().contains(selection))
		{
			return blueAvatarTex;
		} else
		{
			return whiteAvatarTex;
		}

	}

	public GameObject getAvatar()
	{
		return avatar;
	}

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

	public Matrix4f getPlayerRotation()
	{
		return avatar.getLocalRotation();
	}

	public Vector3f getLookAt(GameObject go)
	{
		Vector4f fwdDirection = new Vector4f(0f, 0f, 1f, 1f).mul(go.getWorldRotation());
		return go.getWorldLocation().add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
	}

	/**
	 * Gets the lookat target of the player. This is used for the ghost avatars.
	 * 
	 * @return The lookat target
	 */
	public Vector3f getPlayerLookAt()
	{
		return getLookAt(avatar);
	}

	public String getAvatarSelection()
	{
		return textureSelection;
	}

	public void setIsConnected(boolean value)
	{
		this.isClientConnected = value;
	}

	public void sendByeMessage()
	{
		if (protocolClient != null && isClientConnected == true)
		{
			protocolClient.sendByeMessage();
		}
	}

	public ArrayList<Vector2f> getNpcTargets()
	{
		return targets;
	}

	public void prepareForRace()
	{
		protocolClient.sendPrepRaceMessage();
		// goToRaceStart(-2);
	}

	public void goToRaceStart(float pos)
	{
		racePrep = true;
		Matrix4f mat = new Matrix4f().translate(0, 0, pos * -2);
		avatar.setLocalTranslation(mat);
		avatar.lookAt(-1, 0, pos);
		mat = avatar.getLocalTranslation();
		mat = mat.mul(avatar.getLocalRotation());
		avatarP.setTransform(toDoubleArray(mat.get(vals)));
		avatarP.setAngularVelocity(new float[] { 0, 0, 0 });
		avatarP.setLinearVelocity(new float[] { 0, 0, 0 });
		for (int i = 0; i < 4; i++)
		{
			vehicle.setBrake(Float.MAX_VALUE, i);
			vehicle.applyEngineForce(0, i);
			vehicle.setSteeringValue(0, i);
		}

		if (isNpcHandler)
		{
			NpcAvatar npc = npcManager.getNpc();
			mat = new Matrix4f().translate(0, 0, 2);
			npc.setLocalTranslation(mat);
			npc.lookAt(-1, 0, 2);
			mat = npc.getLocalTranslation();
			mat = mat.mul(npc.getLocalRotation());
			npcP.setTransform(toDoubleArray(mat.get(vals)));
			npcP.setAngularVelocity(new float[] { 0, 0, 0 });
			npcP.setLinearVelocity(new float[] { 0, 0, 0 });
			for (int i = 0; i < 4; i++)
			{
				npcVehicle.setBrake(Float.MAX_VALUE, i);
				npcVehicle.applyEngineForce(0, i);
				npcVehicle.setSteeringValue(0, i);
			}
		}
	}

	public void startRace()
	{
		race = true;
		racePrep = false;

		for (int i = 0; i < 4; i++)
		{
			vehicle.setBrake(0, i);
		}

		if (isNpcHandler)
		{
			for (int i = 0; i < 4; i++)
			{
				npcVehicle.setBrake(0, i);
			}
		}
	}

	// ------------------ INPUT HANDLING ------------------------
	private class ToggleCameraType extends AbstractInputAction {
		MyGame myGame;

		ToggleCameraType(MyGame myGame)
		{
			this.myGame = myGame;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt)
		{
			myGame.toggleCamera();
		}
	}

	private class ToggleAnimationType extends AbstractInputAction {
		MyGame myGame;

		ToggleAnimationType(MyGame myGame)
		{
			this.myGame = myGame;
		}

		@Override
		public void performAction(float time, net.java.games.input.Event evt)
		{
			myGame.ToggleAnimation();
		}
	}

	private class DecreaseVolume extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt)
		{
			if (volume > 0)
			{
				volume -= 0.1;
				updateVolume();
			}
		}
	}

	private class IncreaseVolume extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt)
		{
			if (volume < 1)
			{
				volume += 0.1;
				updateVolume();
			}
		}
	}

	private class PrepRaceAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt)
		{
			prepareForRace();
		}
	}

	// ------------------ UTILITY FUNCTIONS used by physics
	public static javax.vecmath.Vector3f toJavaxVecmath(Vector3f jomlVec)
	{
		return new javax.vecmath.Vector3f(jomlVec.x, jomlVec.y, jomlVec.z);
	}

	private float[] toFloatArray(double[] arr)
	{
		if (arr == null)
		{
			return null;
		}
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++)
		{
			ret[i] = (float) arr[i];
		}
		return ret;
	}

	private double[] toDoubleArray(float[] arr)
	{
		if (arr == null)
		{
			return null;
		}
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++)
		{
			ret[i] = (double) arr[i];
		}
		return ret;
	}

	javax.vecmath.Matrix4f convertJomlToJavax(Matrix4f m)
	{
		javax.vecmath.Matrix4f convert = new javax.vecmath.Matrix4f();

		convert.m00 = m.m00();
		convert.m01 = m.m01();
		convert.m02 = m.m02();
		convert.m03 = m.m03();
		convert.m10 = m.m10();
		convert.m11 = m.m11();
		convert.m12 = m.m12();
		convert.m13 = m.m13();
		convert.m20 = m.m20();
		convert.m21 = m.m21();
		convert.m22 = m.m22();
		convert.m23 = m.m23();
		convert.m30 = m.m30();
		convert.m31 = m.m31();
		convert.m32 = m.m32();
		convert.m33 = m.m33();

		return convert;
	}
}
