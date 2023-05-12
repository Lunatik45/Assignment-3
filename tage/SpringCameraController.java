package tage;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Matrix4f;
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
public class SpringCameraController {

	private Engine engine;
	private Camera camera;        // the camera being controlled
	private GameObject target;    // the target avatar the camera looks at

	private float springConstant; // Determines how stiff or soft the spring i
	private float dampingFactor;  // Determines how much resistance the spring provides to the movement
	private float distance ;      // Distance between camera and target
	private float deadzone;

	private Vector3f targetPosition;
	private Vector3f velocity;

	/**
	 * Create a new Spring Camera Controller
	 * 
	 * @param cam Camera object
	 * @param tgt Target Game Object
	 * @param e   The game's engine
	 */
	public SpringCameraController(Camera cam, GameObject tgt, Engine e)
	{
		engine = e;
		camera = cam;
		target = tgt;
		springConstant = 30.0f;
		dampingFactor = 13.0f;
		distance = 1.75f;        // distance from camera to avatar
		deadzone =  0.5f;
		velocity = new Vector3f();
		
		// Calculate target position
		Vector4f u = new Vector4f(-1f, 0f, 0f, 1f);
		Vector4f v = new Vector4f(0f, 1f, 0f, 1f);
		Vector4f n = new Vector4f(0f, 0f, 1f, 1f);
		u.mul(target.getWorldRotation());
		v.mul(target.getWorldRotation());
		n.mul(target.getWorldRotation());
		Matrix4f w = target.getWorldTranslation();
		targetPosition = new Vector3f(w.m30(), w.m31(), w.m32());
		targetPosition.add(-n.x() * 2.0f, -n.y() * 2.0f, -n.z() * 2.0f);
		targetPosition.add(v.x() * 0.75f, v.y() * 0.75f, v.z() * 0.75f);
	
		// Set camera location and orientation
		camera.setLocation(targetPosition);
		camera.setU(new Vector3f(u.x(), u.y(), u.z()));
		camera.setV(new Vector3f(v.x(), v.y(), v.z()));
		camera.setN(new Vector3f(n.x(), n.y(), n.z()));
	}

	/**
	 * Initializes the inputs to gamepads.
	 */
	private void setupInputs()
	{
		// can make it so that distance is updated
	}

	/**
	 * Compute the camera’s displacement, then its spring, damping, and total force. 
	 * @param elapsedTime Time since last update (in seconds)
	 */
	public void updateCameraPosition(float elapsedTime, Double avatarSpeed) {
		
		// Calculate target position
		Vector4f u = new Vector4f(-0.5f, 0f, 0f, 1f);
		Vector4f v = new Vector4f(0f, 1f, 0f, 1f);
		Vector4f n = new Vector4f(0f, 0f, 1f, 1f);
		u.mul(target.getWorldRotation());
		v.mul(target.getWorldRotation());
		n.mul(target.getWorldRotation());
		Matrix4f w = target.getWorldTranslation();
		targetPosition = new Vector3f(w.m30(), w.m31(), w.m32());
		targetPosition.add(-n.x() * 2.0f, -n.y() * 2.0f, -n.z() * 2.0f);
		targetPosition.add(v.x() * 0.75f, v.y() * 0.75f, v.z() * 0.75f);
	
		// Update camera orientation initially so that it doesn't turn to the wrong direction initially
		//  camera.setLocation(targetPosition);
		camera.lookAt(targetPosition.add(target.getWorldForwardVector().mul(distance)));

		// Calculate camera displacement and forces
		Vector3f currentPosition = camera.getLocation();

		Vector3f displacement = new Vector3f(targetPosition).sub(currentPosition);
		displacement.sub(new Vector3f(target.getWorldForwardVector()).mul(distance));
		Vector3f springForce = new Vector3f(displacement).mul(springConstant);
		Vector3f dampingForce = new Vector3f(velocity).mul(-dampingFactor);
		Vector3f totalForce = new Vector3f(springForce).add(dampingForce);
	
		if(displacement.length() >  deadzone) {
			// Update velocity and position
			velocity.add(new Vector3f(totalForce).mul(elapsedTime));
			currentPosition.add(new Vector3f(velocity).mul(elapsedTime));

			// Set camera location and orientation
			camera.setLocation(currentPosition);
			camera.lookAt(targetPosition);
		}
	}

	/**
	 * Accesible method to move the camera with mouse move commands.
	 * 
	 * @param deltaX Movement in the X direction
	 * @param deltaY Movement in the Y direction
	 */
	public void mouseMove(float deltaX, float deltaY)
	{
	}
	
	/**
	 * Accesible method to move the camera with mouse scroll wheel.
	 * 
	 * @param scrollAmount The amount to zoom in or out
	 */
	public void mouseZoom(int scrollAmount)
	{
		float step = 0.1f;

		distance += scrollAmount * step;

		if (distance < 0.3f)
		{
			distance = 0.3f;
		} else if (distance > 2f)
		{
			distance = 2f;
		}
	}

	/**
	 * Affects the radius of the camera.
	 */
	private class SpringRadiusAction extends AbstractInputAction {
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

			distance += incAmount;

			if (distance < 0.3f)
			{
				distance = 0.3f;
			} else if (distance > 2.75f)
			{
				distance = 2.75f;
			}
		}
	}
}
