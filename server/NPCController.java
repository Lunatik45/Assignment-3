package server;

import java.util.ArrayList;

import org.joml.Vector2f;
import org.joml.Vector3f;

import tage.Log;
import tage.ai.behaviortrees.BTCompositeType;
import tage.ai.behaviortrees.BTSequence;
import tage.ai.behaviortrees.BehaviorTree;

public class NPCController {

	private ArrayList<Vector2f> targets;
	private BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
	private GameServerUDP server;
	private NPC npc;

	private boolean ready = false;
	private int target;
	private long lastThinkUpdateTime, lastTickUpdateTime;
	private long thinkStartTime, tickStartTime;

	public void start()
	{
		thinkStartTime = System.nanoTime();
		tickStartTime = System.nanoTime();
		lastThinkUpdateTime = thinkStartTime;
		lastTickUpdateTime = tickStartTime;
		npcLoop();
	}

	public void init(GameServerUDP s)
	{
		server = s;
		npc = new NPC();
		target = 0;
	}

	public void setTargets(String data)
	{
		setTargetsFromData(data);
		setupBehaviorTree();
		ready = true;
	}

	private void setTargetsFromData(String data)
	{
		String[] points = data.split(",");
		targets = new ArrayList<>();

		if (points.length % 2 != 0)
		{
			System.out.println("Invalid data. Ignoring the last point.");
			targets.remove(targets.size() - 1);
		}

		for (int i = 0; i < points.length; i += 2)
		{
			targets.add(new Vector2f(Float.parseFloat(points[i]), Float.parseFloat(points[i + 1])));
		}

		Log.trace("added targets.\n");
	}

	public void setupBehaviorTree()
	{
		Log.trace("bt set up.\n");
		bt.insertAtRoot(new BTSequence(10));
		bt.insert(10, new CorrectionNeeded(this, npc, false));
		bt.insert(10, new CorrectDrive(this, server, npc));
	}

	public Vector2f getNextTarget()
	{
		try
		{
			return targets.get(target);
		} catch (IndexOutOfBoundsException e)
		{
			return targets.get(0);
		}
	}

	public void npcLoop()
	{
		Log.trace("starting loop.\n");
		while (true)
		{
			if (ready)
			{
				long currentTime = System.nanoTime();
				float elapsedThinkMilliSecs = (currentTime - lastThinkUpdateTime) / (1000000.0f);
				float elapsedTickMilliSecs = (currentTime - lastTickUpdateTime) / (1000000.0f);

				// Think
				if (elapsedThinkMilliSecs >= 1.0f)
				{
					lastThinkUpdateTime = currentTime;
					bt.update(elapsedThinkMilliSecs);
				}

				// Tick
				if (elapsedTickMilliSecs >= 1.0f)
				{
					lastTickUpdateTime = currentTime;
					server.sendNPCStatus(npc.getStatusMsg());
				}
			}
			Thread.yield();
		}
	}

	public void iterateTarget()
	{
		target++;
	}

	public String getNpcStatus()
	{
		return npc.getStatusMsg();
	}

	public void updateNpc(String data)
	{
		String[] t = data.split(",");
		Vector3f pos = new Vector3f(Float.parseFloat(t[0]), Float.parseFloat(t[1]), Float.parseFloat(t[2]));
		Vector3f look = new Vector3f(Float.parseFloat(t[3]), Float.parseFloat(t[4]), Float.parseFloat(t[5]));
		npc.updateInfo(pos, look);
	}
}
