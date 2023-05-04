package a3;

import java.io.IOException;

import org.joml.Matrix4f;
import org.joml.Vector3f;

// import tage.Log;
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

	public void createGhostAvatar(Vector3f position, Vector3f lookat) throws IOException
	{
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getAvatarTex("CarTextureWhite.png");
		npc = new NpcAvatar(s, t, position, game.getNewEngineSound(), game.getAudioManager());
		Matrix4f initialScale = (new Matrix4f()).scaling(0.25f);
		npc.setLocalScale(initialScale);
		npc.lookAt(lookat);
	}
	
	public void shutdown()
	{
		npc.stopSounds();
	}

	public void updateNpcStatus(boolean wantsAccel, boolean wantsDecel, boolean wantsTurnLeft, boolean wantsTurnRight)
	{
		npc.wantsAccel = wantsAccel;
		npc.wantsDecel = wantsDecel;
		npc.wantsTurnLeft = wantsTurnLeft;
		npc.wantsTurnRight = wantsTurnRight;
	}

	public NpcAvatar getNpc()
	{
		return npc;
	}

	public void updateNpcAvatar(Vector3f position, Vector3f lookat)
	{
		npc.setPosition(position);
		npc.lookAt(lookat);
	}


}
