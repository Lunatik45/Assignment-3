package a3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;

import net.java.games.input.Component.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import tage.audio.AudioManagerFactory;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.Light;
import tage.Log;
import tage.ObjShape;
import tage.SpringCameraController;
import tage.TextureImage;
import tage.VariableFrameRateGame;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.InputManager;
import tage.input.action.AccelAction;
import tage.input.action.DecelAction;
import tage.input.action.TurnLeftAction;
import tage.input.action.TurnRightAction;
import tage.networking.IGameConnection.ProtocolType;
import tage.shapes.ImportedModel;
import tage.shapes.TerrainPlane;

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
	private SpringCameraController springController;
	private File scriptFile;
	private GameObject avatar, terrain, trafficCone;
	private ArrayList<GameObject> stationary, dynamic;
	private GhostManager ghostManager;
	private IAudioManager audioMgr;
	private InputManager im;
	private Light light;
	private ObjShape ghostShape, dolphinShape, terrainShape, trafficConeShape, boxCarShape, building1Shape,
			building2Shape, building3Shape, building4Shape, trafficB3Shape, trafficB2Shape, trafficB1Shape;
	private ProtocolClient protocolClient;
	private ProtocolType serverProtocol;
	private ScriptEngine jsEngine;
	private Sound engineSound, bgSound;
	private ArrayList<Sound> ghostSounds;
	private String serverAddress;
	private TextureImage dolphinTex, ghostTex, terrainTex, terrainHeightMap, trafficConeTex, avatarTex, greenAvatarTex,
			redAvatarTex, blueAvatarTex, whiteAvatarTex, building1Tex, building2Tex, building3Tex, building4Tex, 
			trafficTex;

	private boolean isClientConnected = false;
	private boolean isFalling = false, updateScriptInRuntime;
	private double acceleration, deceleration, stoppingForce, gravity, speed = 0, gravitySpeed = 0, turnConst, turnCoef;
	private double startTime, prevTime, elapsedTime;
	private float elapsed;
	private int lakeIslands;
	private int maxSpeed;
	private int passes = 0;
	private int serverPort;
	private String textureSelection = "";

	public MyGame(String serverAddress, int serverPort, String protocol, int debug)
	{
		super();

		Log.setLogLevel(debug);
		ghostManager = new GhostManager(this);
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
		ghostShape = new ImportedModel("box_car.obj");
		dolphinShape = new ImportedModel("dolphinHighPoly.obj");
		// trafficConeShape = new ImportedModel("trafficCone.obj");
		// terrainShape = new TerrainPlane(1000, 1);
		terrainShape = new TerrainPlane(100);
		boxCarShape = new ImportedModel("box_car.obj");
		building1Shape = new ImportedModel("Building1.obj");
		building2Shape = new ImportedModel("Building2.obj");
		building3Shape = new ImportedModel("Building3.obj");
		building4Shape = new ImportedModel("Building4.obj");
		trafficB3Shape = new ImportedModel("TrafficBarricade3.obj");
		trafficB2Shape = new ImportedModel("TrafficBarricade2.obj");
		trafficB1Shape = new ImportedModel("TrafficBarricade1.obj");
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

		terrain = new GameObject(GameObject.root(), terrainShape, terrainTex);
		terrain.setIsTerrain(true);
		terrain.getRenderStates().setTiling(1);
		terrain.setLocalScale((new Matrix4f()).scale(50, 1, 50));
		terrain.setHeightMap(terrainHeightMap);

		float heightOffGround = -boxCarShape.getLowestVertexY();
		avatar = new GameObject(GameObject.root(), boxCarShape, avatarTex);
		avatar.setLocalScale((new Matrix4f()).scale(0.25f));
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
		stationary.add(newObj);

		newObj = new GameObject(GameObject.root(), building2Shape, building2Tex);
		newObj.setLocalScale((new Matrix4f()).scale(0.06f));
		newObj.setLocalTranslation((new Matrix4f()).translate(4.0f, 0.0f, 6.0f));
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
		newObj = new GameObject(GameObject.root(), trafficB3Shape, trafficTex);
		newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		dynamic.add(newObj);

		newObj = new GameObject(GameObject.root(), trafficB2Shape, trafficTex);
		newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		dynamic.add(newObj);

		newObj = new GameObject(GameObject.root(), trafficB1Shape, trafficTex);
		newObj.setLocalScale((new Matrix4f()).scale(0.25f));
		newObj.setLocalTranslation((new Matrix4f()).translate(0.0f, 0.0f, 0.0f));
		dynamic.add(newObj);
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

		setupNetworking();

		setupSounds();

		// ----------------- initialize camera ----------------
		// positionCameraBehindAvatar();
		Camera mainCamera = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		springController = new SpringCameraController(mainCamera, avatar, engine);
		// initMouseMode();
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

	@Override
	public void shutdown()
	{
		super.shutdown();

		ghostManager.shutdown();

		engineSound.release(audioMgr);
		bgSound.release(audioMgr);
		engineResource.unload();
		bgMusicResource.unload();
		audioMgr.shutdown();

		sendByeMessage();
	}

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
		bgSound.play(40, true);

		engineResource = audioMgr.createAudioResource("assets/sounds/engine-6000.wav", AudioResourceType.AUDIO_SAMPLE);
		engineSound = new Sound(engineResource, SoundType.SOUND_EFFECT, 100, true);
		engineSound.initialize(audioMgr);
		engineSound.setMaxDistance(50.0f);
		engineSound.setMinDistance(3.0f);
		engineSound.setRollOff(2.0f);
		engineSound.setLocation(getPlayerPosition());
		engineSound.play(80, true);

		updateEar();
	}

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

	@Override
	public void update()
	{
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		elapsed = (float) (elapsedTime / 1000.0);

		// build and set HUD
		String speedString = String.format("Speed: %.2f", speed);
		engine.getHUDmanager().setHUD1(speedString, new Vector3f(1, 1, 1), 15, 15);

		// update inputs and camera
		stoppingForce(elapsed);
		applyGravity(elapsed);
		im.update(elapsed);
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

		springController.updateCameraPosition(elapsed, speed);
		updateSounds();
	}

	private void updateSounds()
	{
		updateEar();
		engineSound.setLocation(getPlayerPosition());
		engineSound.setPitch((float) (1 + (speed / maxSpeed) * 1.2));
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

	// --------- Movement Section --------

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

		// gasApplied = true;
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

		// brakeApplied = true;
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

		if (pos.y > floor)
		{
			isFalling = true;
			gravitySpeed += time * gravity;
			pos.y -= gravitySpeed;

			if (pos.y <= floor)
			{
				pos.y = floor;
				isFalling = false;
				gravitySpeed = 0;
				pos.y = floor;
			}

			pos.y -= avatar.getShape().getLowestVertexY() * 0.25f;
			avatar.setLocalLocation(pos);
		}

		else if (pos.y < floor)
		{
			pos.y = floor - avatar.getShape().getLowestVertexY() * 0.25f;
			avatar.setLocalLocation(pos);
			isFalling = false;
		}
	}

	private void updatePosition()
	{
		Vector3f oldPosition = avatar.getWorldLocation();
		Vector4f fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
		fwdDirection.mul(avatar.getWorldRotation());
		fwdDirection.mul((float) (speed * 0.1));
		Vector3f newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
		avatar.setLocalLocation(newPosition);
		protocolClient.sendMoveMessage(newPosition, getPlayerLookAt(), engineSound.getPitch());
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

	/**
	 * Gets the lookat target of the player. This is used for the ghost avatars.
	 * 
	 * @return The lookat target
	 */
	public Vector3f getPlayerLookAt()
	{
		Vector4f fwdDirection = new Vector4f(0f, 0f, 1f, 1f).mul(avatar.getWorldRotation());
		return avatar.getWorldLocation().add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
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
}
