//
// Book:      OpenGL(R) ES 2.0 Programming Guide
// Authors:   Aaftab Munshi, Dan Ginsburg, Dave Shreiner
// ISBN-10:   0321502795
// ISBN-13:   9780321502797
// Publisher: Addison-Wesley Professional
// URLs:      http://safari.informit.com/9780321563835
//            http://www.opengles-book.com
//

// Simple_VertexShader
//
//    This is a simple example that draws a rotating cube in perspective
//    using a vertex shader to transform the object
//

package com.opengles.shadow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.openglesbook.common.ESShapes;
import com.openglesbook.common.ESShader;
//import com.openglesbook.common.ESTransform;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

public class ShadowRenderer implements GLSurfaceView.Renderer
{

    ///
    // Constructor
    //
    public ShadowRenderer(Context context)
    {

    }

    ///
    // Initialize the shader and program object
    //
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        String vShaderStr = 
        		"uniform mat4 uMVPMatrix;                   \n"
                + "uniform mat4 uMVMatrix;                  \n"
                + "uniform mat4 uNormalMatrix;              \n"
                + "attribute vec4 vPosition;                \n"
                + "attribute vec3 vVertexNormal;            \n"
                + "varying vec4 v_Position;                	\n"
                + "varying vec3 v_Normal;                		\n"
                + "void main()                              \n"
                + "{                                        \n"
                + "   gl_Position = uMVPMatrix * vPosition;  	\n"
                + "   v_Normal = normalize(mat3(uNormalMatrix) * vVertexNormal);  \n"
                + "   v_Position = uMVMatrix * vPosition;  		\n"
                + "}                                            \n";

        String fShaderStr = 
        		"precision mediump float;                  \n"
                + "uniform vec4 vVertexColor;             \n"
                + "uniform vec4 vLightColor;               \n"
                + "uniform vec3 vLightPosition;            \n"
                + "uniform vec3 vEyePosition;              \n"
                + "varying vec4 v_Position;               	\n"
                + "varying vec3 v_Normal;                		\n"
                + "void main()                                         \n"
                + "{                                                   \n"
                + "  vec3 lightDirection = vLightPosition - vec3(v_Position);  \n"
                + "  float lightDistance = length(lightDirection); 		      \n"
                + "  lightDirection = lightDirection / lightDistance;  \n"
                + "  float attenuation = 1.0 / (1.0 + 0.5 * lightDistance + 0.25 * lightDistance * lightDistance);  \n"
                + "  vec3 halfVector = normalize(lightDirection + vEyePosition);  \n"
                + "  float diffuse = max(0.0, dot(v_Normal, lightDirection));  \n"
                + "  float specular = max(0.0, dot(v_Normal, halfVector));  \n"
                + "  if (diffuse > 0.0)  									\n"
                + "  	specular = pow(specular, 2.0)*1.0;  				\n"
                + "  else  													\n"
                + "  	specular = 0.0;  									\n"
                + "  vec3 scatteredLight = vec3(0.3) + vec3(vLightColor) * diffuse * attenuation;  \n"
        //        + "  vec3 reflectedLight = vec3(vLightColor) * specular * attenuation;  \n"
        //        + "  vec3 rgb = min(vVertexColor.rgb * scatteredLight + reflectedLight, vec3(1.0));  \n"
                + "  vec3 rgb = min(vVertexColor.rgb * scatteredLight, vec3(1.0));  \n"
                + "  gl_FragColor = vec4(rgb, vVertexColor.a); 					       \n"
                + "}                                                   \n";

        // Load the shaders and get a linked program object
        mProgramObject = ESShader.loadProgram(vShaderStr, fShaderStr);

        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "vPosition");
        mNormalLoc = GLES20.glGetAttribLocation(mProgramObject, "vVertexNormal");
        mColorLoc = GLES20.glGetUniformLocation(mProgramObject, "vVertexColor");
        //mColorLoc = GLES20.glGetAttribLocation(mProgramObject, "vVertexColor");

        // Get the uniform locations
        mMVPLoc = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix");
        mMVLoc = GLES20.glGetUniformLocation(mProgramObject, "uMVMatrix");
        mNormalMaxtrixLoc = GLES20.glGetUniformLocation(mProgramObject, "uNormalMatrix");

        mLightColorLocation  = GLES20.glGetUniformLocation(mProgramObject, "vLightColor");
        mLightPositionLocation  = GLES20.glGetUniformLocation(mProgramObject, "vLightPosition");
        mEyePositionLocation  = GLES20.glGetUniformLocation(mProgramObject, "vEyePosition");

        // Generate the vertex data
        mCube.genCube(2.0f);

        // Starting rotation angle for the cube
        mAngle = 45.0f;

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        mSquare   = new Square(mProgramObject);
    }

    private void update()
    {
        if (mLastTime == 0)
            mLastTime = SystemClock.uptimeMillis();
        long curTime = SystemClock.uptimeMillis();
        long elapsedTime = curTime - mLastTime;
        float deltaTime = elapsedTime / 1000.0f;
        mLastTime = curTime;
      
        float aspect;
        
        // Compute a rotation angle based on time to rotate the cube
        mAngle += (deltaTime * 40.0f);
        if (mAngle >= 360.0f)
            mAngle -= 360.0f;
    }

    ///
    // Draw a triangle using the shader pair created in onSurfaceCreated()
    //
    public void onDrawFrame(GL10 glUnused)
    {
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glFinish();

        update();

        drawFloor();
        GLES20.glFinish();

	    // Translate away from the viewer
//      Matrix.translateM(scratch, 0, 0, 1.0f, 0.0f);

      // Rotate the cube
	  float[] rotate = new float[16];
      Matrix.setRotateM(rotate, 0, mAngle, 0, 1.0f, 0.0f);

      // Combine the rotation matrix with the projection and camera view
      // Note that the mMVPMatrix factor *must be first* in order
      // for the matrix multiplication product to be correct.
      float[] MVPMatrix = new float[16];
      Matrix.multiplyMM(MVPMatrix, 0, mVPMatrix, 0, rotate, 0);

      float[] MVMatrix = new float[16];
      Matrix.multiplyMM(MVMatrix, 0, mViewMatrix, 0, rotate, 0);
      GLES20.glUniformMatrix4fv(mMVLoc, 1, false, MVMatrix, 0);

      float[] NormalMatrix = new float[16];
      float[] inverse = new float[16];
      Matrix.invertM(inverse, 0, MVMatrix, 0);
      Matrix.transposeM(NormalMatrix, 0, inverse, 0);
      GLES20.glUniformMatrix4fv(mNormalMaxtrixLoc, 1, false, NormalMatrix, 0);

        // Load the vertex data
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false,
                0, mCube.getVertices());

        GLES20.glEnableVertexAttribArray(mPositionLoc);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mNormalLoc, 3,
                GLES20.GL_FLOAT, false,
                0, mCube.getNormals());
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mNormalLoc);

        // Load the MVP matrix
        GLES20.glUniformMatrix4fv(mMVPLoc, 1, false, MVPMatrix, 0);

        float cubeColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorLoc, 1, cubeColor, 0);
        //GLES20.glVertexAttrib4fv(mColorLoc, cubeColor, 0);
        //GLES20.glEnableVertexAttribArray(mColorLoc);

        // Draw the cube
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mCube.getNumIndices(),
                GLES20.GL_UNSIGNED_SHORT, mCube.getIndices());

        GLES20.glDisableVertexAttribArray(mPositionLoc);
        GLES20.glDisableVertexAttribArray(mNormalLoc);
        //GLES20.glDisableVertexAttribArray(mColorLoc);

    }

    private void drawFloor()
    {
        float[] ViewMatrix = new float[16];
        float[] MVPMatrix = new float[16];
        float[] model = new float[16];
        float[] MVMatrix = new float[16];

        Matrix.setIdentityM(model, 0);

        Matrix.translateM(model, 0, 0, -2.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(MVPMatrix, 0, mVPMatrix, 0, model, 0);

        Matrix.multiplyMM(MVMatrix, 0, mViewMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(mMVLoc, 1, false, MVMatrix, 0);

        float[] NormalMatrix = new float[16];
        float[] inverse = new float[16];
        Matrix.invertM(inverse, 0, MVMatrix, 0);
        Matrix.transposeM(NormalMatrix, 0, inverse, 0);
        GLES20.glUniformMatrix4fv(mNormalMaxtrixLoc, 1, false, NormalMatrix, 0);

        // Draw square
        mSquare.draw(MVPMatrix);        
    }
    
    ///
    // Handle surface changes
    //
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        mWidth = width;
        mHeight = height;

        // Use the program object
        GLES20.glUseProgram(mProgramObject);

        // Set the viewport
        GLES20.glViewport(0, 0, mWidth, mHeight);

        // Compute the window aspect ratio
        float aspect = (float) mWidth / (float) mHeight;

        // Generate a perspective matrix
        if(aspect <  1)
            Matrix.frustumM(mProjectionMatrix, 0, -aspect,aspect,-1, 1, 1.0f, 10.0f);
        else
            Matrix.frustumM(mProjectionMatrix, 0, -1,1,-1/aspect, 1/aspect, 1.0f, 10.0f);

        // Generate a view matrix to rotate/translate the cube
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, eyeCoords[0], eyeCoords[1], eyeCoords[2],
                0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Compute the final MVP by multiplying the
        // modevleiw and perspective matrices together
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        GLES20.glUniform4fv(mLightColorLocation, 1, lightColor, 0);
        GLES20.glUniform3fv(mLightPositionLocation, 1, lightCoords, 0);
        GLES20.glUniform3fv(mEyePositionLocation, 1, eyeCoords, 0);
    }

    // Handle to a program object
    private int mProgramObject;

    // Attribute locations
    private int mPositionLoc;
    private int mNormalLoc;

    // Uniform locations
    private int mMVPLoc;
    private int mMVLoc;
    private int mNormalMaxtrixLoc;

    // color locations
    private int mColorLoc;
    private int mLightColorLocation;
    private int mLightPositionLocation;
    private int mEyePositionLocation;

    // Vertex data
    private ESShapes mCube = new ESShapes();

    private Square mSquare;

    // Rotation angle
    private float mAngle;

    // Additional Member variables
    private int mWidth;
    private int mHeight;
    private long mLastTime = 0;

    float lightColor[] = { 1.0f,  1.0f, 1.0f, 1.0f};
    float lightCoords[] = { -3.0f,  0.0f, 3.0f };
    float eyeCoords[] = { 0.0f,  0.0f, 5.0f };

    private final float[] mVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
}