package tage;

import org.joml.Vector3f;
import net.java.games.input.Event;
import tage.input.InputManager;
import tage.input.IInputManager.INPUT_ACTION_TYPE;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier;

/**
 * Camera Orbit Controller
 * <p>
 * Initializes a camera orbit controller. Controls are defaulted to only work
 * with gamepads but there are accesible methods for mouse move.
 * 
 * @author Scott Gordon, Eric Rodriguez
 */
public class CameraOrbit3D {

	private Engine engine;
	private Camera camera; // the camera being controlled
	private GameObject target; // the target avatar the camera looks at

	private float cameraAzimuth; // rotation around target Y axis
	private float cameraElevation; // elevation of camera above target
	private float cameraRadius; // distance between camera and target
	private double deadzone;

	/**
	 * Create a new Camera Orbit Controller
	 * 
	 * @param cam Camera object
	 * @param tgt Target Game Object
	 * @param e   The game's engine
	 */
	public CameraOrbit3D(Camera cam, GameObject tgt, Engine e)
	{
		engine = e;
		camera = cam;
		target = tgt;
		cameraAzimuth = 0.0f; // start BEHIND and ABOVE the target
		cameraElevation = 20.0f; // elevation is in degrees
		cameraRadius = 2.0f; // distance from camera to avatar
		deadzone = 0.2;
		setupInputs();
		updateCameraPosition();
	}

	/**
	 * Initializes the inputs to gamepads.
	 */
	private void setupInputs()
	{
		OrbitAzimuthAction azmAction = new OrbitAzimuthAction();
		OrbitRadiusAction radAction = new OrbitRadiusAction();
		OrbitElevationAction eleAction = new OrbitElevationAction();
		InputManager im = engine.getInputManager();
		im.associateActionWithAllGamepads(Identifier.Axis.RX, azmAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.Z, radAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(Identifier.Axis.RY, eleAction, INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	/**
	 * Compute the camera’s azimuth, elevation, and distance, relative to the target
	 * in spherical coordinates, then convert to world Cartesian coordinates and set
	 * the camera position from that.
	 */
	public void updateCameraPosition()
	{
		Vector3f tgtRot = target.getWorldForwardVector();
		double tgtAngle = Math.toDegrees((double) tgtRot.angleSigned(new Vector3f(0,
		0, -1), new Vector3f(0, 1, 0)));
		float totalAz = cameraAzimuth - (float) tgtAngle;
		double theta = Math.toRadians(totalAz);
		double phi = Math.toRadians(cameraElevation);
		float x = cameraRadius * (float) (Math.cos(phi) * Math.sin(theta));
		float y = cameraRadius * (float) (Math.sin(phi));
		float z = cameraRadius * (float) (Math.cos(phi) * Math.cos(theta));
		camera.setLocation(new Vector3f(x, y, z).add(target.getWorldLocation()));
		camera.lookAt(target);
	}

	/**
	 * Accesible method to move the camera with mouse move commands.
	 * 
	 * @param deltaX Movement in the X direction
	 * @param deltaY Movement in the Y direction
	 */
	public void mouseMove(float deltaX, float deltaY)
	{
		float step = 0.1f;
		cameraAzimuth += deltaX * step;
		cameraElevation += deltaY * step;

		if (cameraElevation < 0f)
		{
			cameraElevation = 0f;
		} else if (cameraElevation > 85f)
		{
			cameraElevation = 85f;
		}
		updateCameraPosition();
	}
	
	/**
	 * Accesible method to move the camera with mouse scroll wheel.
	 * 
	 * @param scrollAmount The amount to zoom in or out
	 */
	public void mouseZoom(int scrollAmount)
	{
		float step = 0.1f;

		cameraRadius += scrollAmount * step;

		if (cameraRadius < 0.3f)
		{
			cameraRadius = 0.3f;
		} else if (cameraRadius > 4f)
		{
			cameraRadius = 4f;
		}
		updateCameraPosition();
	}

	/**
	 * Affects the Azimuth of the camera.
	 */
	private class OrbitAzimuthAction extends AbstractInputAction {
		public void performAction(float time, Event event)
		{
			float rotAmount = 0.0f;
			if (event.getValue() < -deadzone)
			{
				rotAmount = 0.2f;
			} else if (event.getValue() > deadzone)
			{
				rotAmount = -0.2f;
			}

			cameraAzimuth += rotAmount;
			cameraAzimuth = cameraAzimuth % 360;

			updateCameraPosition();
		}
	}

	/**
	 * Affects the radius of the camera.
	 */
	private class OrbitRadiusAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event event)
		{
			float incAmount = 0.0f;
			if (event.getValue() < -deadzone)
			{
				incAmount = -0.02f;
			} else if (event.getValue() > deadzone)
			{
				incAmount = 0.02f;
			}

			cameraRadius += incAmount;

			if (cameraRadius < 0.3f)
			{
				cameraRadius = 0.3f;
			} else if (cameraRadius > 4f)
			{
				cameraRadius = 4f;
			}

			updateCameraPosition();
		}
	}

	/**
	 * Affects the elevation of the camera.
	 */
	private class OrbitElevationAction extends AbstractInputAction {
		@Override
		public void performAction(float time, Event event)
		{
			float incAmount = 0.0f;
			if (event.getValue() < -deadzone)
			{
				incAmount = -0.2f;
			} else if (event.getValue() > deadzone)
			{
				incAmount = 0.2f;
			}

			cameraElevation += incAmount;

			if (cameraElevation < 0f)
			{
				cameraElevation = 0f;
			} else if (cameraElevation > 85f)
			{
				cameraElevation = 85f;
			}

			updateCameraPosition();
		}
	}
}
