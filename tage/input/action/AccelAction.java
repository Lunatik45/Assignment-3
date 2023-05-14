package tage.input.action;

import net.java.games.input.Event;

import com.bulletphysics.dynamics.vehicle.RaycastVehicle;

public class AccelAction extends AbstractInputAction {
	
	private RaycastVehicle vehicle;
	private int engineForce, maxSpeed;

	public AccelAction(RaycastVehicle v, int engineForce, int maxSpeed)
	{
		vehicle = v;
		this.engineForce = engineForce;
		this.maxSpeed = maxSpeed;
	}

	@Override
	public void performAction(float time, Event e)
	{
		if (vehicle.getCurrentSpeedKmHour() < maxSpeed)
		{
			this.vehicle.applyEngineForce(engineForce, 2);
			this.vehicle.applyEngineForce(engineForce, 3);
		}
		else
		{
			this.vehicle.applyEngineForce(0, 2);
			this.vehicle.applyEngineForce(0, 3);
		}
	}
}

