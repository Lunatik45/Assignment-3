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

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.input.action.AbstractInputAction;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.InputManager;
import tage.Light;
import tage.networking.IGameConnection.ProtocolType;
import tage.ObjShape;
import tage.shapes.ImportedModel;
import tage.shapes.Plane;
import tage.shapes.Sphere;
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
	private GameObject avatar, plane;
	private GhostManager ghostManager;
	private InputManager im;
	private Light light;
	private ObjShape ghostShape, dolphinShape, planeShape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private ScriptEngine jsEngine;
	private String serverAddress;
	private TextureImage dolphinTex, ghostTex, planeTex;

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
		planeShape = new Plane();
	}

	@Override
	public void loadTextures()
	{
		dolphinTex = new TextureImage("Dolphin_HighPolyUV.png");
		ghostTex = new TextureImage("redDolphin.jpg");
		planeTex = new TextureImage("checkerboardSmall.JPG");
	}

	@Override
	public void buildObjects()
	{
		avatar = new GameObject(GameObject.root(), dolphinShape, dolphinTex);

		plane = new GameObject(GameObject.root(), planeShape, planeTex);
		plane.setLocalScale((new Matrix4f()).scale(50));
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
		TurnRightAction turnAction = new TurnRightAction(this);

		im.associateActionWithAllGamepads(Identifier.Button._1, fwdAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.X, turnAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(Identifier.Key.W, fwdAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(Identifier.Key.D, turnAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		
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
