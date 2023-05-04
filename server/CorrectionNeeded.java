package server;

import org.joml.Vector3f;
import org.joml.Vector2f;

import tage.Log;
import tage.ai.behaviortrees.BTCondition;

public class CorrectionNeeded extends BTCondition {

	private NPCController npcController;
	private NPC npc;
	private double angleMargin = 0.1;
	private double positionMargin = 2.0;
	private boolean straightGiven = false;

	public CorrectionNeeded(NPCController npcController, NPC npc, boolean toNegate)
	{
		super(toNegate);
		this.npcController = npcController;
		this.npc = npc;
	}

	@Override
	protected boolean check()
	{
		// Log.trace("Checking if correction needed\n");
		// Check if NPC has reached target and should iterate to next target
		Vector3f position = npc.getPosition();
		Vector2f pos2f = new Vector2f(position.x, position.z);
		Vector2f nextTarget = npcController.getNextTarget();
		if (pos2f.distance(nextTarget) < positionMargin)
		{
			npcController.iterateTarget();
		}

		// Check if NPC is facing the right direction
		Vector3f lookat = npc.getLookat();
		Vector2f look2f = new Vector2f(lookat.x, lookat.z);
		nextTarget = npcController.getNextTarget();
		Vector2f l = new Vector2f(look2f).sub(pos2f).normalize();
		Vector2f t = new Vector2f(nextTarget).sub(pos2f).normalize();
		double angle = l.angle(t);

		if (Math.abs(angle) > angleMargin)
		{
			if (straightGiven)
			{
				straightGiven = false;
			}
			return true;
		} else
		{
			if (!straightGiven)
			{
				npc.goStraight();
				straightGiven = true;
			}
			return false;
		}
	}

}
