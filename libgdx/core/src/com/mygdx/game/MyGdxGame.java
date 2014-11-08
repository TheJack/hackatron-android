package com.mygdx.game;

import org.w3c.dom.css.Rect;


import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;



public class MyGdxGame implements ApplicationListener, InputProcessor  {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model model;
    private ModelInstance modelInstance;
    
    private Model backDrop;
    private ModelInstance backDropInstance;
    
    private Environment environment;
    //private AnimationController controller;
    
    @Override
    public void create() {    
    	 Gdx.input.setInputProcessor(this);
    	 
        camera = new PerspectiveCamera( 75,
						                Gdx.graphics.getWidth(),
						                Gdx.graphics.getHeight());
        
        // Move the camera 5 units back along the z-axis and look at the origin
        camera.position.set(0f,0f,7f);
        camera.lookAt(0f,0f,0f);
        
        camera.near = 0.1f;
        camera.far = 300.0f;
        ////////////////////////////////////////////////
        ModelBuilder modelBuilder = new ModelBuilder();
        // We pass in a ColorAttribute, making our cubes diffuse ( aka, color ) red.
        // And let openGL know we are interested in the Position and Normal channels
       /* float dist = 20;
        backDrop = modelBuilder.createRect(	-5f, -5f, dist, // 00
			        						5f, -5f, dist, // 10
			        						5f, 5f, dist, // 11
			        						-5f, 5f, dist, // 01
			        						0f, 0f, 1f, // normal
			        						new Material(ColorAttribute.createDiffuse(Color.BLUE)), 
			        						Usage.Position | Usage.Normal);
        backDropInstance = new ModelInstance(backDrop);*/
        
        backDrop = modelBuilder.createBox(	160f, 160f, 1f, 
									        new Material(ColorAttribute.createDiffuse(Color.BLUE)),
									        Usage.Position | Usage.Normal );
        backDropInstance = new ModelInstance(backDrop);
        backDropInstance.transform.translate(0, 0, -150);
        
        //Texture texTile = assetManager.get("textures/gdx.jpg", Texture.class);
        //Material mat = new Material(TextureAttribute.createDiffuse(texTile));
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        modelBatch = new ModelBatch();
        
        UBJsonReader jsonReader = new UBJsonReader();
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        model = modelLoader.loadModel(Gdx.files.getFileHandle("robot2.g3db", FileType.Internal));
        modelInstance = new ModelInstance(model);
        
        ////////////////////////////
        /*ObjLoader loader = new ObjLoader();
        model = loader.loadModel(Gdx.files.internal("robot.obj"));
        modelInstance = new ModelInstance(model);*/
        /////////////////////////////
        
        //fbx-conv is supposed to perform this rotation for you... it doesnt seem to
        //modelInstance.transform.rotate(1, 0, 0, -90);
        //move the model down a bit on the screen ( in a z-up world, down is -z ).
        modelInstance.transform.translate(0, 100, -2);

        // Finally we want some light, or we wont see our color.  The environment gets passed in during
        // the rendering process.  Create one, then create an Ambient ( non-positioned, non-directional ) light.
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1.0f));
        
        /*
        // You use an AnimationController to um, control animations.  Each control is tied to the model instance
        controller = new AnimationController(modelInstance);  
        // Pick the current animation by name
        controller.setAnimation("Bend",1, new AnimationListener(){

            @Override
            public void onEnd(AnimationDesc animation) {
                // this will be called when the current animation is done. 
                // queue up another animation called "balloon". 
                // Passing a negative to loop count loops forever.  1f for speed is normal speed.
                controller.queue("balloon",-1,1f,null,0f);
            }

            @Override
            public void onLoop(AnimationDesc animation) {
                // TODO Auto-generated method stub
                
            }
            
        });*/
    }
    
    

    @Override
    public void dispose() {
        modelBatch.dispose();
        model.dispose();
        backDrop.dispose();
    }

    @Override
    public void render() {
        // You've seen all this before, just be sure to clear the GL_DEPTH_BUFFER_BIT when working in 3D
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        // For some flavor, lets spin our camera around the Y axis by 1 degree each time render is called
        //camera.rotateAround(Vector3.Zero, new Vector3(0,1,0),1f);
        // When you change the camera details, you need to call update();
        // Also note, you need to call update() at least once.
        camera.update();
        
        // You need to call update on the animation controller so it will advance the animation.  Pass in frame delta
        //controller.update(Gdx.graphics.getDeltaTime());
        // Like spriteBatch, just with models!  pass in the box Instance and the environment
        modelBatch.begin(camera);
        modelBatch.render(backDropInstance, environment);
        modelBatch.render(modelInstance, environment);
        modelBatch.end();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public boolean keyDown(int keycode) {
        float moveAmount = 10.0f;
        
        if(keycode == Keys.UP)
        	modelInstance.transform.translate(0, -moveAmount, -2);
        if(keycode == Keys.DOWN)
        	modelInstance.transform.translate(0, moveAmount, -2);
        if(keycode == Keys.LEFT)
        	modelInstance.transform.translate(0, 0, -moveAmount);
        if(keycode == Keys.RIGHT)
        	modelInstance.transform.translate(0, 0, moveAmount);
        
        if(keycode == Keys.A)
        	modelInstance.transform.rotate(0, 1, 0, -moveAmount);
        if(keycode == Keys.D)
        	modelInstance.transform.rotate(0, 1, 0, moveAmount);
        
        return true;
    }

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}



	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		oldX = -1;
		oldY = -1;
		return false;
	}


	int oldX;
	int oldY;
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if( oldX < 0)
		{
			oldX = screenX;
			oldY = screenY;
			return true;
		}
		
		int deltaX = (screenX - oldX) / 10;
		int deltaY = (screenY - oldY) / 10;
		
		oldX = screenX;
		oldY = screenY;
		
		if(deltaX > 20 || deltaY > 20)
			return true;
		
		modelInstance.transform.translate(0, deltaX, deltaY);
        return true;
	}



	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public boolean scrolled(int amount) {
		// TODO Auto-generated method stub
		return false;
	}
}