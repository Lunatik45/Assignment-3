package tage.objectRenderers;
import java.nio.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import org.joml.*;
import tage.shapes.*;
import tage.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.linearmath.Transform;
import javax.vecmath.Quat4f;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;

public class RenderPhysicsObject {
    private GLCanvas myCanvas;
    private Engine engine;

    // allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();     // perspective matrix
	private Matrix4f vMat = new Matrix4f();     // view matrix
	private Matrix4f mMat = new Matrix4f();     // model matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc, tLoc, lLoc, eLoc, fLoc, sLoc, cLoc, hLoc, oLoc;
	private int globalAmbLoc,mambLoc,mdiffLoc,mspecLoc,mshiLoc;

    public RenderPhysicsObject(Engine e) {
        this.engine = e;
    }

    public void render(CollisionObject obj, int renderingProgram, Matrix4f pMat, Matrix4f vMat) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glUseProgram(renderingProgram);
    
        mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
        vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
        pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
    
        RigidBody body = RigidBody.upcast(obj);
        if (body == null) return;

        Transform transform = new Transform();
        body.getMotionState().getWorldTransform(transform);

        javax.vecmath.Vector3f aabbMin = new javax.vecmath.Vector3f();
        javax.vecmath.Vector3f aabbMax = new javax.vecmath.Vector3f();
        body.getCollisionShape().getAabb(transform, aabbMin, aabbMax);

        javax.vecmath.Vector3f halfExtents = new javax.vecmath.Vector3f();
        halfExtents.sub(aabbMax, aabbMin);
        halfExtents.scale(0.5f);

        javax.vecmath.Matrix4f translationMatrix = new javax.vecmath.Matrix4f();
        translationMatrix.setIdentity();
        translationMatrix.setTranslation(transform.origin);

        // Convert javax.vecmath.Matrix4f to JOML Matrix4f
        Matrix4f jomlTranslationMatrix = convertJavaxToJoml(translationMatrix);
        Matrix4f mMat = new Matrix4f();
        mMat.mul(jomlTranslationMatrix);

        gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
        gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

        // Wireframe box vertex data
        float[] vertexData = new float[]{
                -halfExtents.x, -halfExtents.y, -halfExtents.z,
                    halfExtents.x, -halfExtents.y, -halfExtents.z,
                    halfExtents.x,  halfExtents.y, -halfExtents.z,
                -halfExtents.x,  halfExtents.y, -halfExtents.z,
                -halfExtents.x, -halfExtents.y,  halfExtents.z,
                    halfExtents.x, -halfExtents.y,  halfExtents.z,
                    halfExtents.x,  halfExtents.y,  halfExtents.z,
                -halfExtents.x,  halfExtents.y,  halfExtents.z
        };

        // Wireframe box index data
        int[] indexData = new int[]{
                0, 1, 1, 2, 2, 3, 3, 0,
                4, 5, 5, 6, 6, 7, 7, 4,
                0, 4, 1, 5, 2, 6, 3, 7
        };

        // Create and bind the VBO
        int[] vbo = new int[1];
        gl.glGenBuffers(1, vbo, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        gl.glBufferData(GL_ARRAY_BUFFER, vertexData.length * 4, FloatBuffer.wrap(vertexData), GL_STATIC_DRAW);

        // Create and bind the VAO
        int[] vao = new int[1];
        gl.glGenVertexArrays(1, vao, 0);
        gl.glBindVertexArray(vao[0]);

        // Create and bind the EBO
        int[] ebo = new int[1];
        gl.glGenBuffers(1, ebo, 0);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData.length * 4, IntBuffer.wrap(indexData), GL_STATIC_DRAW);

        // Set the vertex attribute pointers
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // Draw the wireframe box
        gl.glDrawElements(GL_LINES, indexData.length, GL_UNSIGNED_INT, 0);

        // Unbind and delete the VAO, VBO, and EBO
        gl.glDisableVertexAttribArray(0);
        gl.glBindVertexArray(0);
        gl.glDeleteVertexArrays(1, vao, 0);
        gl.glDeleteBuffers(1, vbo, 0);
        gl.glDeleteBuffers(1, ebo, 0);
    }

    Matrix4f convertJavaxToJoml(javax.vecmath.Matrix4f m) {
        Matrix4f convert = new Matrix4f();
    
        convert.m00(m.m00); convert.m01(m.m01); convert.m02(m.m02); convert.m03(m.m03);
        convert.m10(m.m10); convert.m11(m.m11); convert.m12(m.m12); convert.m13(m.m13);
        convert.m20(m.m20); convert.m21(m.m21); convert.m22(m.m22); convert.m23(m.m23);
        convert.m30(m.m30); convert.m31(m.m31); convert.m32(m.m32); convert.m33(m.m33);
    
        return convert;
    }
}

        // import com.bulletphysics.collision.dispatch.CollisionObject;
        // import com.bulletphysics.dynamics.DynamicsWorld;
        // iterate over all rigid bodies in the dynamics world
        // for (int i = 0; i < dynamicsWorld.getNumCollisionObjects(); i++) {
        //     CollisionObject obj = dynamicsWorld.getCollisionObjectArray().getQuick(i);
        //     RigidBody body = RigidBody.upcast(obj);
        //     if (body != null && body.getMotionState() != null) {
        //         Transform transform = new Transform();
        //         body.getMotionState().getWorldTransform(transform);
        
        //         javax.vecmath.Matrix4f mMat = new javax.vecmath.Matrix4f();
        //         transform.getMatrix(mMat);


        //     }
        // }