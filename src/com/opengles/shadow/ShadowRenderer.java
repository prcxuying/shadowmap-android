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
import java.nio.IntBuffer;
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
import android.util.Log;

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
                + "uniform mat4 uVMatrix;                   \n"  //view
                + "uniform mat4 uMMatrix;                   \n"  //model
                + "uniform mat4 uNormalMatrix;              \n"
                + "attribute vec4 aPosition;                \n"
                + "attribute vec3 aVertexNormal;            \n"
                + "varying vec4 vProjectPosition;                	\n"
                + "varying vec4 vEyePosition;                	\n"
                + "varying vec4 vWorldPosition;                	\n"
                + "varying vec3 vNormal;                		\n"
                + "void main()                              \n"
                + "{                                        \n"
                + "   gl_Position = uMVPMatrix * aPosition;  	\n"
                + "   vNormal = normalize(mat3(uNormalMatrix) * aVertexNormal);  \n"
                + "   vWorldPosition = uMMatrix * aPosition;  		\n"
                + "   vEyePosition = uMVMatrix * aPosition;  		\n"
                + "   vProjectPosition = uMVPMatrix * aPosition;    \n"
                + "}                                            \n";

        String fShaderStr = 
        		"precision mediump float;                  \n"
                + "uniform vec4 uVertexColor;             \n"
                + "uniform vec4 uLightColor;               \n"
                + "uniform vec3 uLightPosition;            \n"
                + "uniform vec3 uEyePosition;              \n"
                + "uniform mat4 uVMatrix;                  \n"
                + "uniform mat4 uShadowMatrix;                  \n"
                + "uniform mat4 uBiasMatrix;                  \n"
                + "varying vec4 vProjectPosition;                \n"
                + "varying vec4 vEyePosition;                	\n"
                + "varying vec4 vWorldPosition;                	\n"
                + "varying vec3 vNormal;                		\n"
                + "uniform sampler2D uLightMap;                       \n"
                + "void main()                                         \n"
                + "{                                                   \n"
                + "  float isUnShadowed = 1.0;		      \n"
                + "  vec4 shadowCoord = uShadowMatrix * vWorldPosition; 		      \n"
                + "  vec3 lightDirection = vec3(uVMatrix*vec4(uLightPosition,1) - vEyePosition);  \n"
                + "  float lightDistance = length(lightDirection); 		      \n"
                + "  lightDirection = lightDirection / lightDistance;  \n"
                + "  float attenuation = 1.0 / (1.0 + 0.5 * lightDistance + 0.25 * lightDistance * lightDistance);  \n"
                + "  vec3 halfVector = normalize(lightDirection + uEyePosition);  \n"
                + "  float diffuse = max(0.0, dot(vNormal, lightDirection));  \n"
                + "  float specular = max(0.0, dot(vNormal, halfVector));  \n"
                + "  if (diffuse > 0.0)  									\n"
                + "  	specular = pow(specular, 1.0)*12.0;  				\n"
                + "  else  													\n"
                + "  	specular = 0.0;  									\n"
                + "  vec3 scatteredLight = uVertexColor.rgb * vec3(uLightColor) * diffuse * attenuation ;  \n"
                + "  vec3 reflectedLight = vec3(uLightColor) * specular * attenuation;  \n"
                + "  vec3 ambientLight = uVertexColor.rgb * vec3(0.3);  \n"
				+ "  vec4 texCoord = shadowCoord / shadowCoord.w;				\n"
                + "  vec4 depthColor = texture2D(uLightMap, vec2(uBiasMatrix*texCoord));  \n"
                + "  float depth =  depthColor.r + depthColor.g/256.0  + depthColor.b /(256.0*256.0) + depthColor.a/(256.0*256.0*256.0); \n"
                + "  if ( shadowCoord.z / shadowCoord.w > depth && depth > 0.01)  \n"
                + "     isUnShadowed = 0.0;  \n"
                + "  vec3 rgb = min(ambientLight + isUnShadowed*(scatteredLight + reflectedLight), vec3(1.0));  \n"
               + "  gl_FragColor = vec4(rgb, uVertexColor.a); 					       \n"
//                + "  depth = shadowCoord.z / shadowCoord.w;	\n"
//                + "  gl_FragColor = vec4(0.0 + depth, 0.0+depth, 0.0+depth, 0.0+depth);	\n"
//              + "  gl_FragColor = depthColor;	\n"
                + "}                                                   \n";

        // Load the shaders and get a linked program object
        mProgramObject = ESShader.loadProgram(vShaderStr, fShaderStr);
        initEyeShaderProgram();

        // Generate the vertex data
        mCube.genCube(2.0f);
        mSphere.genSphere(12, 0.1f);
        // Starting rotation angle for the cube
        mAngle = 45.0f;

        mSquare   = new Square(mProgramObject);
    }

    private void initEyeShaderProgram()
    {
        // Use the program object
        GLES20.glUseProgram(mProgramObject);

        // Get the attribute locations
        mPositionLoc = GLES20.glGetAttribLocation(mProgramObject, "aPosition");
        mNormalLoc = GLES20.glGetAttribLocation(mProgramObject, "aVertexNormal");
        mColorLoc = GLES20.glGetUniformLocation(mProgramObject, "uVertexColor");
        //mColorLoc = GLES20.glGetAttribLocation(mProgramObject, "uVertexColor");

        // Get the uniform locations
        mMVPLoc = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix");
        mMVLoc = GLES20.glGetUniformLocation(mProgramObject, "uMVMatrix");
        mNormalMatrixLoc = GLES20.glGetUniformLocation(mProgramObject, "uNormalMatrix");
        mVLoc = GLES20.glGetUniformLocation(mProgramObject, "uVMatrix");
        mMLoc = GLES20.glGetUniformLocation(mProgramObject, "uMMatrix");
        mShadowMatrixLoc = GLES20.glGetUniformLocation(mProgramObject, "uShadowMatrix");
        mBiasMatrixLoc = GLES20.glGetUniformLocation(mProgramObject, "uBiasMatrix");
        mLightColorLocation  = GLES20.glGetUniformLocation(mProgramObject, "uLightColor");
        mLightPositionLocation  = GLES20.glGetUniformLocation(mProgramObject, "uLightPosition");
        mEyePositionLocation  = GLES20.glGetUniformLocation(mProgramObject, "uEyePosition");

        // Get the sampler locations
        mShadowMapLocation = GLES20.glGetUniformLocation ( mProgramObject, "uLightMap" );
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
        // Use the program object
        GLES20.glUseProgram(mProgramObject);

        update();

        prepareDrawDepthBuffer();
        GLES20.glUniform3fv(mEyePositionLocation, 1, lightCoords, 0);
        drawFloor(mLightViewMatrix, mLightProjectionMatrix);
        drawCube(mLightViewMatrix, mLightProjectionMatrix);
        drawDepthBuffer();

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
/*
        GLES20.glActiveTexture ( GLES20.GL_TEXTURE0 );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, mShadowMapTexture[0] );
        GLES20.glUniform1i ( mShadowMapLocation, 0 );
        GLES20.glUniform3fv(mEyePositionLocation, 1, lightCoords, 0);
        drawFloor(mLightViewMatrix, mLightProjectionMatrix);
        drawCube(mLightViewMatrix, mLightProjectionMatrix);
*/
        GLES20.glUniform3fv(mEyePositionLocation, 1, eyeCoords, 0);
        GLES20.glActiveTexture ( GLES20.GL_TEXTURE0 );
        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, mShadowMapTexture[0] );
        GLES20.glUniform1i ( mShadowMapLocation, 0 );

        drawFloor(mViewMatrix, mProjectionMatrix);
        drawCube(mViewMatrix, mProjectionMatrix);
//        drawLightSource(mViewMatrix, mProjectionMatrix);
		checkGlError("glFramebufferTexture2D depth");

    }

    private void drawCube(float[] viewMatrix, float[] projectionMatrix)
    {
        // Rotate the cube
        float[] rotate = new float[16];
        Matrix.setRotateM(rotate, 0, mAngle, 0, 1.0f, 0.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        float[] MVPMatrix = new float[16];
        float[] MVMatrix = new float[16];
        Matrix.multiplyMM(MVMatrix, 0, viewMatrix, 0, rotate, 0);
        Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVMatrix, 0);

        GLES20.glUniformMatrix4fv(mMVLoc, 1, false, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mVLoc, 1, false, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mMLoc, 1, false, rotate, 0);

        float[] NormalMatrix = new float[16];
        float[] inverse = new float[16];
        Matrix.invertM(inverse, 0, MVMatrix, 0);
        Matrix.transposeM(NormalMatrix, 0, inverse, 0);
        GLES20.glUniformMatrix4fv(mNormalMatrixLoc, 1, false, NormalMatrix, 0);

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

    private void drawLightSource(float[] viewMatrix, float[] projectionMatrix)
    {
        float[] MVPMatrix = new float[16];
        float[] model = new float[16];
        float[] MVMatrix = new float[16];
        float[] NormalMatrix = new float[16];

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, lightCoords[0], lightCoords[1], lightCoords[2]);

        Matrix.multiplyMM(MVMatrix, 0, viewMatrix, 0, model, 0);
        Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVLoc, 1, false, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mVLoc, 1, false, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mMLoc, 1, false, model, 0);

        float[] inverse = new float[16];
        Matrix.invertM(inverse, 0, MVMatrix, 0);
        Matrix.transposeM(NormalMatrix, 0, inverse, 0);
        GLES20.glUniformMatrix4fv(mNormalMatrixLoc, 1, false, NormalMatrix, 0);

        // Load the vertex data
        GLES20.glVertexAttribPointer(mPositionLoc, 3, GLES20.GL_FLOAT, false,
                0, mSphere.getVertices());

        GLES20.glEnableVertexAttribArray(mPositionLoc);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mNormalLoc, 3,
                GLES20.GL_FLOAT, false,
                0, mSphere.getNormals());
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mNormalLoc);

        // Load the MVP matrix
        GLES20.glUniformMatrix4fv(mMVPLoc, 1, false, MVPMatrix, 0);

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorLoc, 1, lightColor, 0);

        // Draw the cube
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mSphere.getNumIndices(),
                GLES20.GL_UNSIGNED_SHORT, mSphere.getIndices());

        GLES20.glDisableVertexAttribArray(mPositionLoc);
        GLES20.glDisableVertexAttribArray(mNormalLoc);
    }

    private void drawFloor(float[] viewMatrix, float[] projectionMatrix)
    {
        float[] MVPMatrix = new float[16];
        float[] model = new float[16];
        float[] MVMatrix = new float[16];


        Matrix.setIdentityM(model, 0);
        Matrix.scaleM(model, 0, 3f, 1f, 4f);
        Matrix.translateM(model, 0, floorCoords[0], floorCoords[1], floorCoords[2]);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(MVMatrix, 0, viewMatrix, 0, model, 0);
        Matrix.multiplyMM(MVPMatrix, 0, projectionMatrix, 0, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVLoc, 1, false, MVMatrix, 0);
        GLES20.glUniformMatrix4fv(mVLoc, 1, false, viewMatrix, 0);
        GLES20.glUniformMatrix4fv(mMLoc, 1, false, model, 0);

        float[] NormalMatrix = new float[16];
        float[] inverse = new float[16];
        Matrix.invertM(inverse, 0, MVMatrix, 0);
        Matrix.transposeM(NormalMatrix, 0, inverse, 0);
        GLES20.glUniformMatrix4fv(mNormalMatrixLoc, 1, false, NormalMatrix, 0);

        // Draw square
        mSquare.draw(MVPMatrix);        
    }

    private void prepareDrawDepthBuffer()
    {
//        GLES20.glColorMask(false, false, false, false);
        //Depth states
        GLES20.glClearDepthf(1.0f);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, mShadowMapTexture[0], 0);
        // Clear the color buffer
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
		checkGlError("glFramebufferTexture2D depth");
    }

    private void drawDepthBuffer()
    {
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mShadowMapTexture[0]);
//        checkGlError("glCopyTexSubImage2D");
//        GLES20.glCopyTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT, 0, 0, mWidth, mHeight, 0);
//        checkGlError("glCopyTexSubImage2D");
//        GLES20.glColorMask(true, true, true, true);
    }

    ///
    // Handle surface changes
    //
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        mWidth = width;
        mHeight = height;

        setupDepthTexture();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Set the viewport
        GLES20.glViewport(0, 0, mWidth, mHeight);

        // Compute the window aspect ratio
        float aspect = (float) mWidth / (float) mHeight;

        // Generate a perspective matrix
        if(aspect <  1)
            Matrix.frustumM(mProjectionMatrix, 0, -aspect,aspect,-1, 1, 1.0f, 20.0f);
        else
            Matrix.frustumM(mProjectionMatrix, 0, -1,1,-1/aspect, 1/aspect, 1.0f, 20.0f);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, eyeCoords[0], eyeCoords[1], eyeCoords[2],
                0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Generate a perspective matrix
        if(aspect <  1)
            Matrix.frustumM(mLightProjectionMatrix, 0, -aspect,aspect,-1, 1, 1.0f, 20.0f);
        else
            Matrix.frustumM(mLightProjectionMatrix, 0, -1,1,-1/aspect, 1/aspect, 1.0f, 20.0f);

                // Set the camera position (View matrix)
        Matrix.setLookAtM(mLightViewMatrix, 0, lightCoords[0], lightCoords[1], lightCoords[2],
                0f, 0f, 0f, 0f, 1.0f, 0.0f);
        float[] LightVPMatrix = new float[16];
        Matrix.multiplyMM(LightVPMatrix, 0, mLightProjectionMatrix, 0, mLightViewMatrix, 0);

        // Compute the final MVP by multiplying the
        // modevleiw and perspective matrices together
        // Calculate the projection and view transformation

        GLES20.glUniform4fv(mLightColorLocation, 1, lightColor, 0);
        GLES20.glUniform3fv(mLightPositionLocation, 1, lightCoords, 0);
        GLES20.glUniform3fv(mEyePositionLocation, 1, eyeCoords, 0);

        Matrix.setIdentityM(mShadowmapBiasMatrix, 0);
        Matrix.translateM(mShadowmapBiasMatrix, 0, 0.5f, 0.5f, 0.5f);
        Matrix.scaleM(mShadowmapBiasMatrix, 0, 0.5f, 0.5f, 0.5f);
        GLES20.glUniformMatrix4fv(mBiasMatrixLoc, 1, false, mShadowmapBiasMatrix, 0);

        float[] shadowProjectM = new float[16];
        Matrix.multiplyMM(shadowProjectM, 0, mShadowmapBiasMatrix, 0, LightVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mShadowMatrixLoc, 1, false, LightVPMatrix, 0);
    }

	private void setupDepthTexture() {
		// generate
		checkGlError("depthBuffer");
		GLES20.glDeleteFramebuffers(1, fb, 0);
		checkGlError("depthBuffer");
		GLES20.glDeleteTextures(1, mShadowMapTexture, 0);
		GLES20.glGenFramebuffers(1, fb, 0);
		GLES20.glGenTextures(1, mShadowMapTexture, 0);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mShadowMapTexture[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_REPEAT);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_REPEAT);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_NEAREST);

		// create it
		int zero[] = new int[mWidth * mHeight];

		IntBuffer depthBuffer = ByteBuffer.allocateDirect(mWidth * mHeight * 4).asIntBuffer();
		depthBuffer.put(zero).position(0);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT, mWidth, mHeight, 0,
					GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_INT, depthBuffer);
		checkGlError("depthBuffer");
	}

	// debugging opengl
	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
		//	throw new RuntimeException(op + ": glError " + error);
		}
	}

    // Handle to a program object
    private int mProgramObject;
    private int mLightProgramObject;
    // Attribute locations
    private int mPositionLoc;
    private int mNormalLoc;

    // Uniform locations
    private int mMVPLoc;
    private int mMVLoc;
    private int mNormalMatrixLoc;
    private int mVLoc;
    private int mMLoc;
    private int mShadowMatrixLoc;
    private int mBiasMatrixLoc;

    // color locations
    private int mColorLoc;
    private int mLightColorLocation;
    private int mLightPositionLocation;
    private int mEyePositionLocation;
    private int mShadowMapLocation;

    // Vertex data
    private ESShapes mCube = new ESShapes();
    private ESShapes mSphere = new ESShapes();

    private Square mSquare;

    // Rotation angle
    private float mAngle;

    private int[] mShadowMapTexture = new int[1];
    private int[] fb = new int[1];

    // Additional Member variables
    private int mWidth;
    private int mHeight;
    private long mLastTime = 0;

    float floorCoords[] = { 0.0f,  -4.0f, 0.0f };
    float lightColor[] = { 1.0f,  1.0f, 1.0f, 1.0f};
    float lightCoords[] = { 3.0f,  6.0f, 3.0f };
    float eyeCoords[] = { 0.0f,  5.0f, 4.0f };
    float cubeCoords[] = {0.0f,  0.0f, 0.0f};

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mLightProjectionMatrix = new float[16];
    private final float[] mLightViewMatrix = new float[16];
    private final float[] mShadowmapBiasMatrix = new float[16];
}
