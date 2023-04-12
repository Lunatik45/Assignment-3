package a3;

import java.io.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.java.games.input.Component.Identifier;
import tage.*;
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.Light;
import tage.Log;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.input.InputManager;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.action.AbstractInputAction;
import tage.input.action.AccelAction;
import tage.input.action.FwdAction;
import tage.input.action.BwdAction;
import tage.input.action.DecelAction;
import tage.input.action.TurnLeftAction;
import tage.input.action.TurnRightAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.shapes.ImportedModel;
import tage.shapes.Sphere;
import tage.shapes.TerrainPlane;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tage.TextureImage;
import tage.VariableFrameRateGame;

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

	private File scriptFile;
	private GameObject avatar, terrain, trafficCone;
	private GhostManager ghostManager;
	private InputManager im;
	private Light light;
	private ObjShape ghostShape, dolphinShape, terrainShape, trafficConeShape, boxCarShape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private ScriptEngine jsEngine;
	private String serverAddress;
	private TextureImage dolphinTex, ghostTex, terrainTex, terrainHeightMap, trafficConeTex, boxCarTex;
	private int lakeIslands;
	private boolean isClientConnected = false;
	private int serverPort;
	private CameraOrbit3D orbitController;
	private boolean isFalling = false;
	private double acceleration, deceleration, stoppingForce, gravity, speed = 0, gravitySpeed = 0, turnConst, turnCoef;
	private double startTime, prevTime, elapsedTime;
	private float elapsed;
	private int maxSpeed;
	private boolean updateScriptInRuntime, allowLogLevelChange;
	private int passes = 0;

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
			System.out.println("Note: Script will update during runtime.\nCAUTION: Performance may be affected while this mode is in use");
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
		// terrainShape = new TerrainPlane(1000, 1);
		terrainShape = new TerrainPlane(1000);
		boxCarShape = new ImportedModel("box_car.obj");
	}

	@Override
	public void loadTextures()
	{
		dolphinTex = new TextureImage("Dolphin_HighPolyUV.png");
		trafficConeTex = new TextureImage("traffic_cone.png");
		ghostTex = new TextureImage("redDolphin.jpg");
		terrainTex = new TextureImage("tileable_grass_01.png");
		terrainHeightMap = new TextureImage("terrain1.jpg");
		boxCarTex = new TextureImage("CarTexture.png");
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
		avatar = new GameObject(GameObject.root(), boxCarShape, boxCarTex);
		avatar.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		avatar.setLocalScale((new Matrix4f()).scale(0.25f));

		trafficCone = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		trafficCone.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.65f, 0.0f));
		trafficCone.setLocalScale((new Matrix4f()).scale(0.25f, 0.25f, 0.25f));

		terrain = new GameObject(GameObject.root(), terrainShape, terrainTex);

		terrain.setIsTerrain(true);
		terrain.getRenderStates().setTiling(1);
		terrain.setLocalScale((new Matrix4f()).scale(50, 4, 50));
		terrain.setHeightMap(terrainHeightMap);
		terrain.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 0f));
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
		orbitController = new CameraOrbit3D(mainCamera, avatar, engine);
		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();
		AccelAction accelAction = new AccelAction(this, protocolClient);
		DecelAction decelAction = new DecelAction(this, protocolClient);
		TurnRightAction turnRightAction = new TurnRightAction(this, (float) turnConst, (float) turnCoef);
		TurnLeftAction turnLeftAction = new TurnLeftAction(this, (float) turnConst, (float) turnCoef);

		im.associateActionWithAllGamepads(Identifier.Button._1, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Identifier.Key.W, accelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, decelAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, turnLeftAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	public GameObject getAvatar()
	{
		return avatar;
	}

	@Override
	public void update()
	{
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		elapsed = (float) (elapsedTime / 1000.0);

		// build and set HUD
		String speedString = String.format("Speed: %.2f", speed);
		engine.getHUDmanager().setHUD1(speedString, new Vector3f(1, 1, 1), 15, 15);


		// My code start -- update avatar to move up with terrain
		// update altitude of dolphin based on height map
		// Vector3f loc = avatar.getWorldLocation();
		// float height = terr.getHeight(loc.x(), loc.z());
		// avatar.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		// update inputs and camera
		stoppingForce(elapsed);
		applyGravity(elapsed);
		im.update(elapsed);
		// positionCameraBehindAvatar();
		updatePosition();
		processNetworking(elapsed);

		if (updateScriptInRuntime)
		{
			passes++;

			if (passes > 30)
			{
				updateScripts();
			}
		}
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

}
