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
import tage.Light.LightType;
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
import tage.shapes.Plane;
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
	private GameObject avatar, terrain, terrainQ1, terrainQ2, terrainQ3, terrainQ4, plane, myRoad, frontRW,
			frontLW, backRW, backLW, waypoint, tcBarrier1, tcBarrier2, tcBarrier3, tcBarrier4, carLight_L, carLight_R, spareTire;
	private AnimatedShape avatarAS;
	private GhostManager ghostManager;
	private IAudioManager audioMgr;
	private InputManager im;
	private Light light, car_L, car_R;
	private ObjShape ghostShape, planeShape, terrainShape, terrainQ1S, terrainQ2S, terrainQ3S, terrainQ4S,
			trafficConeShape, boxCarShape, myRoadShape, frontRWShape, frontLWShape, backRWShape, backLWShape,
			building1Shape, building2Shape, building3Shape, building4Shape, trafficB3Shape, trafficB2Shape,
			trafficB1Shape, arrowShape, multipleBuildings, multipleBuildings3, multipleBuildings4, carLightShape_L, carLightShape_R, spareTireShape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private Robot robot;
	private ScriptEngine jsEngine;
	private Sound engineSound, bgSound;
	private ArrayList<Sound> ghostSounds;
	private String serverAddress;
	private TextureImage ghostTex, terrainTex, trafficConeTex, boxCarTex, myRoadTex, avatarTex,
			greenAvatarTex, redAvatarTex, blueAvatarTex, whiteAvatarTex, terrainHeightMap, building1Tex, building2Tex,
			building3Tex, building4Tex, trafficTex, arrowTex;
	private NpcManager npcManager;
	private RaycastVehicle vehicle, npcVehicle;
	private ArrayList<Vector2f> targets;
	private Vector2f targetPos;
	private ArrayList<GameObject> allWaypoints;

	private boolean isClientConnected = false, isNpcHandler = false, race = false, racePrep = false, raceDone = false,
			disableMouse = false, seeAllWaypoints = false;
	private float vals[] = new float[16];
	private boolean isFalling = false, updateScriptInRuntime, newTarget = true;
	private double centerX, centerY, prevMouseX, prevMouseY, curMouseX, curMouseY;
	private double gravity, speed = 0, turnConst, turnCoef, turnMax;
	private double startTime, prevTime, elapsedTime, amt, volume = 1, totalTime;
	private float elapsed, targetMargin = 25, waypointHeight = 7f;
	private int maxVolBG, maxVolEng, arid, maxSpeed, engineForce, brakeForce, passes = 0, target = 0, position = 0;
	private int serverPort, avatarPhysicsUID, npcPhysicsUID;
	private PhysicsEngine physicsEngine;
	private PhysicsObject avatarP, trafficConeP, terrainP, frontRWP, frontLWP, backRWP, backLWP, npcP, building2P;
	private PhysicsHingeConstraint frontRWHinge, frontLWHinge, backRWHinge, backLWHinge;
	private Boolean toggleCameraType = false;
	private Boolean toggleAnimation = false;
	private Boolean mouseIsRecentering = false;
	private String textureSelection = "";

	private float ds, dx, dy, dz, dr;
	private GameObject dob;

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

		setupTargets();
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
		trafficConeShape = new ImportedModel("trafficCone.obj");
		terrainShape = new TerrainPlane(500);
		planeShape = new Plane();
		spareTireShape = new ImportedModel("spareTire.obj");
		myRoadShape = new ImportedModel("myRoad.obj");
		building1Shape = new ImportedModel("Building1.obj");
		building2Shape = new ImportedModel("Building2.obj");
		building3Shape = new ImportedModel("Building3.obj");
		building4Shape = new ImportedModel("Building4.obj");
		trafficB3Shape = new ImportedModel("TrafficBarricade3.obj");
		trafficB2Shape = new ImportedModel("TrafficBarricade2.obj");
		trafficB1Shape = new ImportedModel("TrafficBarricade1.obj");
		arrowShape = new ImportedModel("arrow.obj");
		multipleBuildings = new ImportedModel("MultipleBuildings.obj");
		multipleBuildings3 = new ImportedModel("MultipleBuildings3.obj");
		carLightShape_L = new ImportedModel("leftCarLight.obj");
		carLightShape_R = new ImportedModel("rightCarLight.obj");
		// multipleBuildings4 = new ImportedModel("MultipleBuildings4.obj");
	}

	@Override
	public void loadTextures()
	{
		trafficConeTex = new TextureImage("traffic_cone.png");
		ghostTex = new TextureImage("CarTexture.png");
		terrainTex = new TextureImage("tileable_grass_02.png");
		terrainHeightMap = new TextureImage("terrainTest.png");
		boxCarTex = new TextureImage("CarTexture.png");
		myRoadTex = new TextureImage("road1.jpg");
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
		arid = (engine.getSceneGraph()).loadCubeMap("arid");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(arid);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{
		stationary = new ArrayList<GameObject>();
		dynamic = new ArrayList<GameObject>();
		GameObject newObj;

		plane = new GameObject(GameObject.root(), planeShape, terrainTex);
		plane.getRenderStates().setTiling(1);
		plane.setLocalTranslation((new Matrix4f()).translate(0f, -.06f, 0f));
		plane.setLocalScale((new Matrix4f()).scale(1000.0f, -.09f, 1000.0f));

		terrain = new GameObject(GameObject.root(), terrainShape, terrainTex);
		terrain.getRenderStates().setTiling(1);
		terrain.setHeightMap(terrainHeightMap);
		terrain.setLocalTranslation((new Matrix4f()).translate(-500f, -.02f, -500f));
		terrain.setLocalScale((new Matrix4f()).scale(500.0f, 4.0f, 500.0f));

		float heightOffGround = -avatarAS.getLowestVertexY();
		avatar = new GameObject(GameObject.root(), avatarAS, avatarTex);
		avatar.setLocalScale((new Matrix4f()).scale(.25f, .25f, .25f));
		avatar.setLocalTranslation((new Matrix4f()).translate(0.0f, heightOffGround, 0.0f));
		// avatar.setLocalTranslation((new Matrix4f()).translate(-230.0f, heightOffGround, -480.0f));

		spareTire = new GameObject(avatar, spareTireShape, avatarTex);
		// spareTire.setLocalScale((new Matrix4f()).scale(.25f, .25f, .25f));

		carLight_L = new GameObject(avatar, carLightShape_L, avatarTex);
		carLight_L.getRenderStates().disableRendering();
		carLight_R = new GameObject(avatar, carLightShape_R, avatarTex);
		carLight_R.getRenderStates().disableRendering();

		// Template:
		// newObj = new GameObject(GameObject.root(), shape, tex);
		// newObj.setLocalScale((new Matrix4f()).scale(1f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		// stationary.add(newObj);

		// Add object primarily meant to be stationary
		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-180.0f, 0.0f, 37.0f));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-428.0f, 0.0f, 6.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(-153.55)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-620.0f, 0.0f, 90.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(14)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-900.0f, 0.0f, 250.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(90)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-903.5f, 0.0f, 450.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(-90)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-780.0f, 0.0f, 563.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-285.5f, 0.0f, 559.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-589.0f, 0.0f, 631.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-12.0f, 0.0f, 425.5f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(385.0f, 0.0f, 390.5f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(-100)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building1Shape, building1Tex);
		newObj.setLocalScale((new Matrix4f()).scale(140f));
		newObj.setLocalTranslation((new Matrix4f()).translate(250.0f, 0.0f, 160.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(-45)));
		stationary.add(newObj);

		// Only stationary obj with a physics object aside from the map boundaries
		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 13.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-60.0f, 0.0f, 13.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-140.0f, 0.0f, -63.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(0)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-495.0f, 0.0f, 91.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(25)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		newObj.setLocalTranslation((new Matrix4f()).translate(-1001.0f, 0.0f, 225.0f));
		newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(90)));
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), multipleBuildings, building2Tex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.5f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(-1001.0f, 0.0f, 285.0f));
		// newObj.setLocalRotation((new Matrix4f()).rotateY((float) Math.toRadians(90)));
		stationary.add(newObj);
		
		// dob = new GameObject(GameObject.root(), building2Shape, building2Tex);

		newObj = new GameObject(GameObject.root(), multipleBuildings3, building3Tex);

		// newObj = new GameObject(GameObject.root(), building3Shape, building3Tex);
		// newObj.setLocalScale((new Matrix4f()).scale(0.06f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(4.0f, 0.0f, 0.0f));
		// stationary.add(newObj);
		
		// newObj = new GameObject(GameObject.root(), multipleBuildings4, building4Tex);

		// newObj = new GameObject(GameObject.root(), building4Shape, building4Tex);
		// newObj.setLocalScale((new Matrix4f()).scale(3.0f));
		// newObj.setLocalTranslation((new Matrix4f()).translate(-5.0f, 0.0f, 15.0f));
		// stationary.add(newObj);

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
		waypoint.setLocalScale((new Matrix4f()).scale(7.0f));

		if (seeAllWaypoints)
		{
			allWaypoints = new ArrayList<>();
			for (int i = 0; i < targets.size(); i++)
			{
				GameObject newWaypoint = new GameObject(GameObject.root(), arrowShape, arrowTex);
				newWaypoint.setLocalScale((new Matrix4f()).scale(4.0f));
				newWaypoint.getRenderStates().setWireframe(true);
				newWaypoint.setLocalTranslation(
						(new Matrix4f()).translate(targets.get(i).x, waypointHeight, targets.get(i).y));
				// newWaypoint.getRenderStates().disableRendering();
				allWaypoints.add(newWaypoint);
			}
		}

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
		tcBarrier1.getRenderStates().disableRendering();

		tcBarrier2 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier2.getRenderStates().disableRendering();

		tcBarrier3 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier3.getRenderStates().disableRendering();

		tcBarrier4 = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		tcBarrier4.getRenderStates().disableRendering();

		// terrain = new GameObject(GameObject.root(), terrainShape);
		// terrain.getRenderStates().setWireframe(true);

		// terrain.setIsTerrain(true);
		// terrain.getRenderStates().setTiling(1);
		// terrain.setLocalScale((new Matrix4f()).scale(50, 5, 50));
		// terrain.setHeightMap(terrainHeightMap);
		// terrain.setLocalTranslation((new Matrix4f()).translation(0f, -2f, 0f));
	}

	@Override
	public void initializeLights()
	{
		// Light.setGlobalAmbient(.5f, .5f, .5f);
		
		// light = new Light();
		// light.setLocation(new Vector3f(0f, 5f, 0f));

		car_L = new Light();
		car_R = new Light();

		// engine.getSceneGraph().addLight(light);
		engine.getSceneGraph().addLight(car_L);
		engine.getSceneGraph().addLight(car_R);	
		
		car_L.setAmbient(0.3f, 0.3f, 0.3f);
		car_L.setDiffuse(0.8f, 0.8f, 0.8f);
		car_L.setSpecular(1.0f, 1.0f, 1.0f);

		car_R.setAmbient(0.3f, 0.3f, 0.3f);
		car_R.setDiffuse(0.8f, 0.8f, 0.8f);
		car_R.setSpecular(1.0f, 1.0f, 1.0f);

		car_L.setType(Light.LightType.POSITIONAL);
		car_R.setType(Light.LightType.POSITIONAL);
		
		car_L.setCutoffAngle(30.0f);
		car_R.setCutoffAngle(30.0f);

		car_L.setOffAxisExponent(2.0f);
		car_R.setOffAxisExponent(2.0f);
		
		car_L.setDirection(new Vector3f(-1f, 0f, 0f));
		car_R.setDirection(new Vector3f(-1f, 0f, 0f));

		Vector3f left = carLight_L.getLocalLocation();
		car_L.setLocation(left);
		Vector3f right = carLight_R.getLocalLocation();
		car_R.setLocation(right);

		car_R.setRange(5.0f);
		car_L.setRange(5.0f);
	}

	@Override
	public void initializeGame()
	{
		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		engine.getRenderSystem().setWindowDimensions(1900, 1000);
		engine.getRenderSystem().setLocationRelativeTo(null);
		mainCamera = (engine.getRenderSystem().getViewport("MAIN").getCamera());

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

		translation = new Matrix4f(terrain.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		com.bulletphysics.linearmath.Transform moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(-500, -1, -500); 
		// terrainP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(),
		// tempTransform, up, 0.0f);
		float [] terrainSize = {500, 1f , 500f};
		terrainP = physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform, terrainSize);
		terrain.setPhysicsObject(physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), tempTransform, up, 0.0f));
		terrainP.setFriction(0.20f);

		translation = new Matrix4f();
		tempTransform = toDoubleArray(translation.get(vals));
		float [] planeSize = {2000, 1f , 2000f};
		moveTo = new com.bulletphysics.linearmath.Transform();
        moveTo.setIdentity();
        moveTo.origin.set(0, -1, 0); 
		myRoad.setPhysicsObject(physicsEngine.addBoxObject(physicsEngine.nextUID(), 0, tempTransform, planeSize));
		terrainP.setFriction(0.75f);

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
        moveTo.origin.set(980f, 20f, 0f);
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
        moveTo.origin.set(-990.0f, 20.0f, 1.0f);
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
		AccelAction accelAction = new AccelAction(vehicle, engineForce, maxSpeed);
		DecelAction decelAction = new DecelAction(vehicle, brakeForce);
		TurnRightAction turnRightAction = new TurnRightAction((float) turnConst, (float) turnCoef, (float) turnMax, vehicle);
		TurnLeftAction turnLeftAction = new TurnLeftAction((float) turnConst, (float) turnCoef, (float) turnMax, vehicle);
		ToggleCameraType toggleCameraType = new ToggleCameraType(this);
		IncreaseVolume increaseVolume = new IncreaseVolume();
		DecreaseVolume decreaseVolume = new DecreaseVolume();
		ToggleAnimationType toggleAnimationType = new ToggleAnimationType(this);
		PrepRaceAction prepRaceAction = new PrepRaceAction();
		ToggleMouse toggleMouse = new ToggleMouse();

		im.associateActionWithAllGamepads(Identifier.Button._1, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);


		im.associateActionWithAllKeyboards(Identifier.Key.W, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, decelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, turnLeftAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.O, increaseVolume, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key.L, decreaseVolume, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		im.associateActionWithAllKeyboards(Identifier.Key.M, toggleMouse, INPUT_ACTION_TYPE.ON_PRESS_ONLY);
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

			// Skip the code below and go to the next GameObject if the PO is null
			if (po == null)
				continue;

			// If the game object is a terrain then handle height then skip the rest of the code and go to the next game object
			if(go.isTerrain())
			{
				Vector3f loc = avatar.getWorldLocation();
				// Terrain is offset by -500 in the x and z direction to get it into a specific quadrant
				float height = terrain.getHeight(500f + loc.x(), 500f + loc.z());
				float[] vals = new float[16];
				Matrix4f m = new Matrix4f().translation(loc.x, height+.5f, loc.z);
				po.setTransform(toDoubleArray(m.get(vals)));

				continue;
			}

			// we allow the terrain static object to be checked to be updated but all other static objects don't need to be moved
			if (!po.isDynamic())
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

			if(po.isVehicle()) 
			{
				Vector3f updateLight_L = carLight_L.getWorldLocation();
				Vector3f updateLight_R = carLight_R.getWorldLocation();
				
				car_L.setLocation(updateLight_L);
				car_R.setLocation(updateLight_R);

				car_L.setDirection(carLight_L.getLocalForwardVector());
				car_R.setLocation(carLight_R.getLocalForwardVector());

			}
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
		String hud = String.format("Speed: %.2f", speed);
		String p = "";
		if (raceDone)
		{
			p = String.format("Finished %d!", position);
		}
		else{
			p = String.format("Position: %d", position);
		}
		engine.getHUDmanager().setHUD1(hud, new Vector3f(1, 1, 1), 15, 15);
		engine.getHUDmanager().setHUD2(p, new Vector3f(1, 1, 1), 550, 15);

		checkForNewTarget();

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
		targets.add(new Vector2f(-200.5f, 3.0f));
		targets.add(new Vector2f(-349f, 8.5f));
		targets.add(new Vector2f(-545f, 102f));
		targets.add(new Vector2f(-646f, 130.5f));
		targets.add(new Vector2f(-874.5f, 188.5f));
		targets.add(new Vector2f(-926.5f, 215.5f));
		targets.add(new Vector2f(-936f, 260f));
		targets.add(new Vector2f(-936f, 420f));
		targets.add(new Vector2f(-938f, 532f));
		targets.add(new Vector2f(-925f, 583f));
		targets.add(new Vector2f(-880.5f, 595.5f));
		targets.add(new Vector2f(-636f, 601.5f));
		targets.add(new Vector2f(-562f, 585f));
		targets.add(new Vector2f(-549f, 517f));
		targets.add(new Vector2f(-550f, 441f));
		targets.add(new Vector2f(-522f, 403f));
		targets.add(new Vector2f(-403.5f, 389f));
		targets.add(new Vector2f(-347.5f, 414f));
		targets.add(new Vector2f(-338.5f, 532f));
		targets.add(new Vector2f(-298f, 589.5f));
		targets.add(new Vector2f(-207f, 590.5f));
		targets.add(new Vector2f(-157f, 570f));
		targets.add(new Vector2f(-159f, 474f));
		targets.add(new Vector2f(-143f, 416.5f));
		targets.add(new Vector2f(-8.5f, 390.5f));
		targets.add(new Vector2f(261.5f, 393f));
		targets.add(new Vector2f(339f, 376.5f));
		targets.add(new Vector2f(354.5f, 259.5f));
		targets.add(new Vector2f(324.5f, 193.5f));
		targets.add(new Vector2f(184f, 48f));
		targets.add(new Vector2f(169f, 2f));
		targets.add(new Vector2f(459.5f, -176f));
		targets.add(new Vector2f(568.5f, -194.5f));
		targets.add(new Vector2f(727.5f, -53.5f));
		targets.add(new Vector2f(805.5f, -16f));
		targets.add(new Vector2f(884f, -13.5f));
		targets.add(new Vector2f(940.5f, -39.5f));
		targets.add(new Vector2f(941.5f, -255f));
		targets.add(new Vector2f(945.5f, -412.5f));
		targets.add(new Vector2f(924.5f, -475f));
		targets.add(new Vector2f(841.5f, -482.5f));
		targets.add(new Vector2f(609.5f, -479.5f));
		targets.add(new Vector2f(429.5f, -479.5f));
		targets.add(new Vector2f(239.5f, -479.5f));
		targets.add(new Vector2f(170f, -479.5f));
		targets.add(new Vector2f(-79f, -479.5f));
		targets.add(new Vector2f(-186f, -455f));
		targets.add(new Vector2f(-279.5f, -407f));
		targets.add(new Vector2f(-320.5f, -354.5f));
		targets.add(new Vector2f(-324f, -264f));
		targets.add(new Vector2f(-278.5f, -221.5f));
		targets.add(new Vector2f(15.5f, -230.5f));
		targets.add(new Vector2f(73.5f, -192.5f));
		targets.add(new Vector2f(83.5f, -42f));
		targets.add(new Vector2f(23f, 0.5f));
		targets.add(new Vector2f(0f, 0f));
		// targets.add(new Vector2f( f, f));
	}

	private void checkForNewTarget()
	{
		if (newTarget)
		{
			if (target >= targets.size())
			{
				targetPos = new Vector2f(0, 0);
				protocolClient.sendFinishedRaceMessage();
				raceDone = true;
			} else
			{
				targetPos = targets.get(target);
			}
			waypoint.setLocalLocation(new Vector3f(targetPos.x, waypointHeight, targetPos.y));
			newTarget = false;
		}
	}

	public void setPosition(int position)
	{
		this.position = position;
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
		return (int) Math.floor(maxVolBG * volume);
	}

	public int getEngVolume()
	{
		return (int) Math.floor(maxVolEng * volume);
	}

	public void updateVolume()
	{
		npcManager.updateVolume();
		ghostManager.updateVolume();
		bgSound.setVolume(getBgVolume());
		engineSound.setVolume(getEngVolume());
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
			v.applyEngineForce(engineForce, 2);
			v.applyEngineForce(engineForce, 3);
			v.setBrake(0, 2);
			v.setBrake(0, 3);
		} else if (npc.wantsDecel)
		{
			v.setBrake(brakeForce, 2);
			v.setBrake(brakeForce, 3);
		} else
		{
			v.applyEngineForce(0, 2);
			v.applyEngineForce(0, 3);
			v.setBrake(0, 2);
			v.setBrake(0, 3);
		}

		if ((npc.wantsTurnLeft || npc.wantsTurnRight) && v.getCurrentSpeedKmHour() < 5)
		{
			v.applyEngineForce(engineForce * 0.15f, 2);
			v.applyEngineForce(engineForce * 0.15f, 3);
			v.setBrake(0, 2);
			v.setBrake(0, 3);
		}

		float f = v.getSteeringValue(0);
		float b;
		if (npc.wantsTurnLeft)
		{
			f += turnConst + (turnCoef * time);
			if (f > turnMax)
			{
				f = (float) turnMax;
			}
		} else if (npc.wantsTurnRight)
		{
			f -= turnConst + (turnCoef * time);
			if (f < -turnMax)
			{
				f = (float) -turnMax;
			}
		} else
		{
			f = 0;
		}
		
		b = f * -0.5f;

		v.setSteeringValue(f, 0);
		v.setSteeringValue(f, 1);
		v.setSteeringValue(b, 2);
		v.setSteeringValue(b, 3);

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
				protocolClient.sendCheckpointMessage(target);
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

	public void toggleMouse()
	{
		disableMouse = !disableMouse;

		RenderSystem rs = engine.getRenderSystem();
		if (disableMouse)
		{
			Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
			rs.getGLCanvas().setCursor(normalCursor);
		} else
		{
			BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "blank cursor");
			rs.getGLCanvas().setCursor(blankCursor);
		}
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
		if (!disableMouse)
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
	}

	@Override
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
		engineForce = (Integer) jsEngine.get("engineForce");
		brakeForce = (Integer) jsEngine.get("brakeForce");
		gravity = (Double) jsEngine.get("gravity");
		turnConst = (Double) jsEngine.get("turnConst");
		turnCoef = (Double) jsEngine.get("turnCoef");
		turnMax = (Double) jsEngine.get("turnMax");
		maxVolBG = (Integer) jsEngine.get("bgSound");
		maxVolEng = (Integer) jsEngine.get("engSound");
		seeAllWaypoints = (Boolean) jsEngine.get("seeAllWaypoints");
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
		avatar.lookAt(-1, 0, pos * -2);
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

	private class ToggleMouse extends AbstractInputAction {
		@Override
		public void performAction(float time, Event evt)
		{
			toggleMouse();
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
