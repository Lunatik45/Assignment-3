package server;

import org.joml.Vector2f;
import org.joml.Vector3f;

import tage.Log;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

public class CorrectDrive extends BTAction{

	private NPCController npcController;
	private GameServerUDP server;
	private NPC npc;
	private double m1 = 0.1, m2 = 0.3, m3 = 1;

	public CorrectDrive(NPCController npcController, GameServerUDP server, NPC npc)
	{
		super();

		this.npcController = npcController;
		this.server = server;
		this.npc = npc;
	}

	@Override
	protected BTStatus update(float elapsedTime)
	{
		Vector3f lookat = npc.getLookat();
		Vector2f look2f = new Vector2f(lookat.x, lookat.z);
		Vector3f position = npc.getPosition();
		Vector2f pos2f = new Vector2f(position.x, position.z);
		Vector2f nextTarget = npcController.getNextTarget();
		Vector2f l = new Vector2f(look2f).sub(pos2f).normalize();
		Vector2f t = new Vector2f(nextTarget).sub(pos2f).normalize();
		float angle = l.angle(t);
		double angleAbs = Math.abs(angle);
		boolean left = angle < 0;

		// Log.print("Angle: %.2f\n", angle);

		npc.resetDriveStatus();

		if (angleAbs > m3)
		{
			npc.brake();
			npc.turn(left);
		}
		else if (angleAbs > m2)
		{
			npc.turn(left);
		}
		else if (angleAbs > m1)
		{
			npc.accelerate();
			npc.turn(left);
		}
		else
		{
			npc.accelerate();
		}

		return BTStatus.BH_SUCCESS;
	}
}
