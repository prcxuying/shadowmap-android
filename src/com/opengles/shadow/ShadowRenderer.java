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
                + "uniform mat3 uNormalMatrix;              \n"
                + "uniform vec4 vVertexColor;             \n"
                + "attribute vec3 vVertexNormal;            \n"
                + "attribute vec4 vPosition;                \n"
                + "varying vec4 Color;                		\n"
                + "varying vec4 Position;                	\n"
                + "varying vec3 Normal;                		\n"
                + "void main()                              \n"
                + "{                                        \n"
                + "   Color = vVertexColor;  				\n"
                + "   Normal = normalize(uNormalMatrix * vVertexNormal);  \n"
                + "   Position = uMVMatrix * vPosition;  		\n"
                + "   gl_Position = uMVPMatrix * vPosition;  	\n"
                + "}                                            \n";

        String fShaderStr = 
        		"precision mediump float;                  \n"
                + "uniform vec4 vLightColor;               \n"
                + "uniform vec4 vLightPosition;            \n"
                + "uniform vec4 vShininess;                \n"
                + "uniform vec4 vStrengh;                  \n"
                + "uniform vec4 vEyePosition;              \n"
                + "uniform vec4 vColor;                		\n"
                + "varying vec4 Color;                		\n"
                + "varying vec4 Position;               	\n"
                + "varying vec4 Normal;                		\n"
                + "void main()                                         \n"
                + "{                                                   \n"
                + "  gl_FragColor = Color; 					       \n"
                + "}                                                   \n";

        // Load the shaders and get a linked program object
        mProgramObject = ESShader.loadProgram(vShaderStr, fShaderStr);

        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "vPosition");

        // Get the uniform locations
        mMVPLoc = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix");

        // Get the uniform locations
 //       mColorLoc = GLES20.glGetUniformLocation(mProgramObject, "vColor");

        mColorLoc = GLES20.glGetUniformLocation(mProgramObject, "vVertexColor");
        
        // Generate the vertex data
        mCube.genCube(1.0f);

        // Starting rotation angle for the cube
        mAngle = 45.0f;

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 0.0f);
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
        float[] rotate = new float[16];
        
        // Compute a rotation angle based on time to rotate the cube
        mAngle += (deltaTime * 40.0f);
        if (mAngle >= 360.0f)
            mAngle -= 360.0f;

        // Compute the window aspect ratio
        aspect = (float) mWidth / (float) mHeight;
        
        // Generate a perspective matrix           
        if(aspect <  1)
        	Matrix.frustumM(mProjectionMatrix, 0, -aspect,aspect,-1, 1, 1.0f, 20.0f);
        else
        	Matrix.frustumM(mProjectionMatrix, 0, -1,1,-1/aspect, 1/aspect, 1.0f, 20.0f);
        
        // Generate a view matrix to rotate/translate the cube
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, eyeCoords[0], eyeCoords[1], eyeCoords[2], 
    			0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Compute the final MVP by multiplying the
        // modevleiw and perspective matrices together
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        
        
        // Translate away from the viewer
//        modelview.translate(1.5f, 0.0f, -5.0f);
//        Matrix.translateM(scratch, 0, 0, 1.0f, 0.0f);
        
        // Rotate the cube
//        modelview.rotate(45, 1.0f, 0.0f, 1.0f);
        Matrix.setRotateM(rotate, 0, mAngle, 0, 1.0f, 1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(mMVPMatrix, 0, mVPMatrix, 0, rotate, 0);
        
        

    }

    ///
    // Draw a triangle using the shader pair created in onSurfaceCreated()
    //
    public void onDrawFrame(GL10 glUnused)
    {
        // Use the program object
        GLES20.glUseProgram(mProgramObject);
        
    	update();
		drawFloor();
        // Set the viewport
        GLES20.glViewport(0, 0, mWidth, mHeight);

        // Clear the color buffer
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Load the vertex data
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false,
                0, mCube.getVertices());
        GLES20.glEnableVertexAttribArray(mPositionLoc);

        // Load the MVP matrix
        GLES20.glUniformMatrix4fv(mMVPLoc, 1, false, mMVPMatrix, 0);

        float cubeColor[] = {1.0f, 0.0f, 0.0f, 0,0}; 
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorLoc, 1, cubeColor, 0);
        
        // Draw the cube
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mCube.getNumIndices(),
                GLES20.GL_UNSIGNED_SHORT, mCube.getIndices());
    }

    private void drawFloor()
    {
        float[] ViewMatrix = new float[16];
        float[] MVPMatrix = new float[16];
        float[] scratch = new float[16];

        Matrix.setIdentityM(scratch, 0);
        
        Matrix.translateM(scratch, 0, 0, 0.0f, -5.0f);
        
        // Calculate the projection and view transformation
        Matrix.multiplyMM(MVPMatrix, 0, mVPMatrix, 0, scratch, 0);
        
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
    }

    // Handle to a program object
    private int mProgramObject;

    // Attribute locations
    private int mPositionLoc;

    // Uniform locations
    private int mMVPLoc;

    // color locations
    private int mColorLoc;

    // Vertex data
    private ESShapes mCube = new ESShapes();

    private Square mSquare;
    
    // Rotation angle
    private float mAngle;

    // Additional Member variables
    private int mWidth;
    private int mHeight;
    private long mLastTime = 0;
        
    float lightCoords[] = { -2.0f,  4.0f, -5.0f }; 
    float eyeCoords[] = { 1.0f,  2.0f, 5.0f };

    private final float[] mVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    private final float[] mMVMatrix = new float[16];
}