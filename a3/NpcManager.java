package a3;

import java.io.IOException;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.Log;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;

public class NpcManager {

	private MyGame game;
	private NpcAvatar npc = null;

	public NpcManager(VariableFrameRateGame vfrg)
	{
		game = (MyGame) vfrg;
	}

	public void createNpcAvatar(Vector3f position, Vector3f lookat) throws IOException
	{
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getAvatarTex("CarTextureWhite.png");
		npc = new NpcAvatar(s, t, position, lookat, game.getNewEngineSound(), game.getAudioManager());
		npc.setPhysicsObject(game.getNpcPhysicsObject());
		// Matrix4f initialScale = (new Matrix4f()).scaling(0.25f);
		// npc.setLocalScale(initialScale);
		// npc.lookAt(lookat);
		npc.setVolume(game.getEngVolume());
		Log.trace("NPC created\n");
	}

	public void shutdown()
	{
		if (npc != null)
		{
			npc.stopSounds();
		}
	}

	public void updateNpcStatus(boolean wantsAccel, boolean wantsDecel, boolean wantsTurnLeft, boolean wantsTurnRight)
	{
		if (npc != null)
		{
			npc.wantsAccel = wantsAccel;
			npc.wantsDecel = wantsDecel;
			npc.wantsTurnLeft = wantsTurnLeft;
			npc.wantsTurnRight = wantsTurnRight;
		}
	}

	public NpcAvatar getNpc()
	{
		return npc;
	}

	public void updateNpcAvatar(Vector3f position, Vector3f lookat)
	{
		if (npc != null)
		{
			npc.setPosition(position, lookat);
		}
	}

	public void updateVolume()
	{
		if (npc != null)
		{
			npc.setVolume(game.getEngVolume());
		}
	}

	public void updateSounds()
	{
		if (npc != null)
		{
			npc.updateSounds();
		}
	}
}
