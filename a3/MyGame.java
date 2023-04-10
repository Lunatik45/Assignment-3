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
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.Light;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.input.InputManager;
import tage.input.TurnRightAction;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.action.AbstractInputAction;
import tage.input.action.FwdAction;
import tage.input.action.BwdAction;
import tage.input.action.TurnLeftAction;
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
	private ObjShape ghostShape, dolphinShape, terrainShape, trafficConeShape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private ScriptEngine jsEngine;
	private String serverAddress;
	private TextureImage dolphinTex, ghostTex, terrainTex, terrainHeightMap, trafficConeTex;
	private int lakeIslands;
	private boolean isClientConnected = false;
	private double startTime, prevTime, elapsedTime;
	private int serverPort;
	private int maxSpeed;
	private double acceleration, stoppingForce, gravity, speed = 0;

	public MyGame(String serverAddress, int serverPort, String protocol)
	{
		super();

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
	}

	public static void main(String[] args)
	{
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
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
		terrainShape = new TerrainPlane(20);
	}

	@Override
	public void loadTextures()
	{
		dolphinTex = new TextureImage("Dolphin_HighPolyUV.png");
		trafficConeTex = new TextureImage("traffic_cone.png");
		ghostTex = new TextureImage("redDolphin.jpg");
		// terrainTex = new TextureImage("small_checkerboard.png");
		terrainTex = new TextureImage("tileable_grass_01.png");
		terrainHeightMap = new TextureImage("terrain1.jpg");
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

		avatar = new GameObject(GameObject.root(), dolphinShape, dolphinTex);
		avatar.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		trafficCone = new GameObject(GameObject.root(), trafficConeShape, trafficConeTex);
		trafficCone.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.65f, 0.0f));
		trafficCone.setLocalScale((new Matrix4f()).scale(0.25f, 0.25f, 0.25f));
		terrain = new GameObject(GameObject.root(), terrainShape, terrainTex);

		terrain.setHeightMap(terrainHeightMap);
		terrain.setLocalTranslation((new Matrix4f()).translation(0f, 0f, 0f));

		terrain.setLocalScale((new Matrix4f()).scale(20.0f, 8.0f, 20.0f));

		terrain.getRenderStates().setTiling(1);
		// terrainShape.setTextureCoordinateScale(1.0f/(1000.0f/64.0f));
		// terrain.setTextureImage(terrainTex);
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

		// ----------------- JS section ------------------
		runScript(scriptFile);
		maxSpeed = (Integer) jsEngine.get("maxSpeed");
		acceleration = (Double) jsEngine.get("acceleration");
		stoppingForce = (Double) jsEngine.get("stoppingForce");
		gravity = (Double) jsEngine.get("gravity");

		// ----------------- initialize camera ----------------
		positionCameraBehindAvatar();

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();

		FwdAction fwdAction = new FwdAction(this, protocolClient);
		BwdAction bwdAction = new BwdAction(this, protocolClient);
		TurnRightAction turnRightAction = new TurnRightAction(this);
		TurnLeftAction turnLeftAction = new TurnLeftAction(this);

		im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Identifier.Key.W, fwdAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, turnRightAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.A, turnLeftAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.S, bwdAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
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

		// build and set HUD
		engine.getHUDmanager().setHUD1(Double.toString(speed), new Vector3f(1, 1, 1), 15, 15);


		// My code start -- update avatar to move up with terrain
		// update altitude of dolphin based on height map
		// Vector3f loc = avatar.getWorldLocation();
		// float height = terr.getHeight(loc.x(), loc.z());
		// avatar.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		// update inputs and camera
		im.update((float) elapsedTime);
		positionCameraBehindAvatar();
		processNetworking((float) elapsedTime);
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

	// ---------- SCRIPTING SECTION ----------------

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

	private void updateScripts()
	{
		runScript(scriptFile);

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
