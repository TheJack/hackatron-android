package com.mygdx.game;

import java.util.ArrayList;
import java.util.Random;

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
    private Model voltron;
    private ModelInstance voltronInstance;
    
    private Model backDrop;
    private ModelInstance backDropInstance;
    
    private Model asteroid;
    private ArrayList<ModelInstance> asteroidInstances;
    
    private Environment environment;
    //private AnimationController controller;
    
    private Random random;
    
    @Override
    public void create() {   
    	random = new Random();
    	Gdx.input.setInputProcessor(this);
    	 
        camera = new PerspectiveCamera( 75,
						                Gdx.graphics.getWidth(),
						                Gdx.graphics.getHeight());
        
        // Move the camera 5 units back along the z-axis and look at the origin
        camera.position.set(0f,0f,7f);
        camera.lookAt(0f,0f,0f);
        
        camera.near = 0.1f;
        camera.far = 1000.0f;
        
        ////////////////////////////////////////////////
        // We want some light, or we wont see our color.  The environment gets passed in during
        // the rendering process.  Create one, then create an Ambient ( non-positioned, non-directional ) light.
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1.0f));
       
        ////////////////////////////////////////////////
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBatch = new ModelBatch();
        
        ////////////////////////////////////////////////
        Texture texTile = new Texture(Gdx.files.getFileHandle("background.jpg", FileType.Internal));
        backDrop = modelBuilder.createBox(	1300f, 800f, 1f, 
        									new Material(TextureAttribute.createDiffuse(texTile)),
									        Usage.Position | Usage.Normal | Usage.TextureCoordinates );
        backDropInstance = new ModelInstance(backDrop);
        backDropInstance.transform.translate(0, 0, -450);
        
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        UBJsonReader jsonReader = new UBJsonReader();
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        
        // Note, the model (g3db file ) and textures need to be added to the assets folder of the Android proj
        voltron = modelLoader.loadModel(Gdx.files.getFileHandle("robot2.g3db", FileType.Internal));
        voltronInstance = new ModelInstance(voltron);
        voltronInstance.transform.scale(0.5f, 0.5f, 0.5f);
        voltronInstance.transform.translate(0, -20, -320);
        
        asteroid = modelLoader.loadModel(Gdx.files.getFileHandle("asteroid.g3db", FileType.Internal));
        
        asteroidInstances = new ArrayList<ModelInstance>();
        int asteroidsCount = 5;
        for(int i = 0; i < asteroidsCount; ++i){
        	asteroidInstances.add(new ModelInstance(asteroid));
        	
        	asteroidInstances.get(i).transform.translate(i * 60 - 100, 0, -200);
        }
    }
    
    @Override
    public void dispose() {
        modelBatch.dispose();
        voltron.dispose();
        backDrop.dispose();
        asteroid.dispose();
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
        
        for(ModelInstance instance : asteroidInstances){
        	instance.transform.rotate(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1);
        	modelBatch.render(instance, environment);
        }
        
        modelBatch.render(voltronInstance, environment);
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
        	voltronInstance.transform.translate(0, -moveAmount, -2);
        if(keycode == Keys.DOWN)
        	voltronInstance.transform.translate(0, moveAmount, -2);
        if(keycode == Keys.LEFT)
        	voltronInstance.transform.translate(0, 0, -moveAmount);
        if(keycode == Keys.RIGHT)
        	voltronInstance.transform.translate(0, 0, moveAmount);
        
        if(keycode == Keys.A)
        	voltronInstance.transform.rotate(0, 1, 0, -moveAmount);
        if(keycode == Keys.D)
        	voltronInstance.transform.rotate(0, 1, 0, moveAmount);
        
        return true;
    }

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
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
		
		voltronInstance.transform.translate(0, deltaX, deltaY);
        return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}