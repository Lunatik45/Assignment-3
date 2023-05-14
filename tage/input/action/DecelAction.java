package tage.input.action;

import net.java.games.input.Event;

import com.bulletphysics.dynamics.vehicle.RaycastVehicle;

public class DecelAction extends AbstractInputAction {
	
	private RaycastVehicle vehicle;
	private int brakeForce;

	public DecelAction(RaycastVehicle v, int brakeForce)
	{
		vehicle = v;
		this.brakeForce = brakeForce;
	}

	@Override
	public void performAction(float time, Event e)
	{
		this.vehicle.applyEngineForce(0, 2);
		this.vehicle.applyEngineForce(0, 3);
		this.vehicle.setBrake(brakeForce, 2);
		this.vehicle.setBrake(brakeForce, 3);
	}
}