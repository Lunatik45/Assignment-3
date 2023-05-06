package a3;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.Log;
import tage.ObjShape;
import tage.TextureImage;
import tage.VariableFrameRateGame;

public class GhostManager {
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(VariableFrameRateGame vfrg)
	{
		game = (MyGame) vfrg;
	}

	public void createGhostAvatar(UUID id, Vector3f position, Vector3f lookat, String texture) throws IOException
	{
		System.out.println("adding ghost with ID: " + id);
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getAvatarTex(texture);
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position, game.getNewEngineSound(), game.getAudioManager());
		Matrix4f initialScale = (new Matrix4f()).scaling(0.25f);
		newAvatar.setLocalScale(initialScale);
		newAvatar.lookAt(lookat);
		ghostAvatars.add(newAvatar);
	}

	public void removeGhostAvatar(UUID id)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{
			ghostAvatar.stopSounds();	
			game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		} else
		{
			System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{
		GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while (it.hasNext())
		{
			ghostAvatar = it.next();
			if (ghostAvatar.getID().compareTo(id) == 0)
			{
				return ghostAvatar;
			}
		}
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position, Vector3f lookat, float pitch)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{
			ghostAvatar.setPosition(position);
			ghostAvatar.lookAt(lookat);
			ghostAvatar.setSoundPitch(pitch);
		} else
		{
			System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	/**
	 * Shutdown command primarily used to stop all sounds from playing.
	 */
	public void shutdown()
	{
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while (it.hasNext())
		{
			it.next().stopSounds();
		}
	}
}

