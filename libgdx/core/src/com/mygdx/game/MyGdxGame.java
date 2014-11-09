package com.mygdx.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

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
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.particles.emitters.Emitter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;



public class MyGdxGame implements ApplicationListener, InputProcessor  {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    
    private Model voltron;
    private ModelInstance voltronInstance;
    
    private Model leftArm;
    private ModelInstance leftArmInstance;
    
    private Model rightArm;
    private ModelInstance rightArmInstance;
    
    private Model backDrop;
    private ModelInstance backDropInstance;
    
    
    final static float AsteroidFallSpeed = 40f;
    private Model asteroid;
    private int asteroidsCount = 0;
    private ArrayList<ModelInstance> asteroidInstances;
    private ArrayList<Vector3> 		 asteroidPositions;
    private FloatArray 		 		 asteroidFallSpeeds;
    
    private Environment environment;
    //private AnimationController controller;
    
    private Random random;
    private boolean gameOver;
    
    private float bodyXPosition;
    private float bodyYPosition;
    private float bodyZPosition;
    private float leftArmRotation;
    private float rightArmRotation;
    
    final static float SideScrollLimit = 14.0f;
    final static float ArmRotationLimit = 45.0f;
    final static float ArmsOffset = 0.5f;
    final static float ArmsStartRotation = 135;
    
    private SpriteBatch batch;
	private final int ExplosionsPoolSize = 50;
	private Array<ParticleEffect> explosionsEffects;
	private ParticleEffectPool explosionEffectPool;
    
    @Override
    public void create() {
    	gameOver = false;
    	bodyXPosition = 0f;
    	bodyYPosition = -4.2f;
    	bodyZPosition = -4.8f;
    	leftArmRotation = ArmsStartRotation;
    	rightArmRotation = ArmsStartRotation;
    	
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
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        
        ////////////////////////////////////////////////
        batch = new SpriteBatch();

        explosionsEffects = new Array<ParticleEffect>();
        ParticleEffect explosionEffect = new ParticleEffect();
        explosionEffect.load(Gdx.files.internal("explosion.p"), Gdx.files.internal("img"));
        explosionEffect.start();
        
        explosionEffectPool = new ParticleEffectPool(explosionEffect, 1, ExplosionsPoolSize);

        ////////////////////////////////////////////////
		ModelBuilder modelBuilder = new ModelBuilder();
        modelBatch = new ModelBatch();
        
        Texture texTile = new Texture(Gdx.files.getFileHandle("background.jpg", FileType.Internal));
        backDrop = modelBuilder.createBox(	1300f, 800f, 1f, 
        									new Material(TextureAttribute.createDiffuse(texTile)),
									        Usage.Position | Usage.Normal | Usage.TextureCoordinates );
        backDropInstance = new ModelInstance(backDrop);
        backDropInstance.transform.translate(0, 0, -450);
        
        ////////////////////////////////////////////////
        UBJsonReader jsonReader = new UBJsonReader();
        G3dModelLoader modelLoader = new G3dModelLoader(jsonReader);
        
        voltron = modelLoader.loadModel(Gdx.files.getFileHandle("body.g3db", FileType.Internal));
        voltronInstance = new ModelInstance(voltron);
        voltronInstance.transform.rotate(0, 1, 0, 180);

        leftArm = modelLoader.loadModel(Gdx.files.getFileHandle("left-arm.g3db", FileType.Internal));
        leftArmInstance = new ModelInstance(leftArm);
        leftArmInstance.transform.translate(ArmsOffset, bodyYPosition, bodyZPosition);
        MoveLeftArm(0.5f);
        
        rightArm = modelLoader.loadModel(Gdx.files.getFileHandle("right-arm.g3db", FileType.Internal));
        rightArmInstance = new ModelInstance(rightArm);
        rightArmInstance.transform.translate(-ArmsOffset, bodyYPosition, bodyZPosition);
        MoveRightArm(0.5f);
        
        MoveBody(0.5f);
        ////////////////////////////////////////////////
        asteroid = modelLoader.loadModel(Gdx.files.getFileHandle("asteroid.g3db", FileType.Internal));
        
        asteroidInstances = new ArrayList<ModelInstance>();
        asteroidPositions = new ArrayList<Vector3>();
        asteroidFallSpeeds = new FloatArray();
        /*
        for(int i = 0; i < asteroidsCount; ++i){
        	asteroidInstances.add(new ModelInstance(asteroid));
        	asteroidPositions.add(NewSpawnPosition());
        	
        	//asteroidInstances.get(i).transform.translate(i * 60 - 150, 0, -200);
        	//asteroidPositions.add(new Vector3(i * 60f - 150f, 0f, -200f));
        }*/
    }
    
    @Override
    public void dispose() {
        modelBatch.dispose();
        voltron.dispose();
        backDrop.dispose();
        asteroid.dispose();
        leftArm.dispose();
        rightArm.dispose();
    }
    
    public void SpawnAsteroid(float x, float y, float fallSpeed){
    	asteroidInstances.add(new ModelInstance(asteroid));
    	
    	Vector3 pos = new Vector3(	(x - 0.5f) * 400,				// horizontal spread 
									bodyYPosition + y * 180 + 120, // vertical spread
									-200f );
    	
    	asteroidPositions.add(pos);
    	asteroidFallSpeeds.add(fallSpeed);
    	++asteroidsCount;
    }
    
    private void UpdateAsteroids(float dt){
    	for(int i = 0; i < asteroidsCount; ++i){
    		asteroidPositions.get(i).y  -= dt * asteroidFallSpeeds.get(i);
        	asteroidInstances.get(i).transform.setTranslation(asteroidPositions.get(i));
        	
        	if(asteroidPositions.get(i).y < bodyYPosition - 180){
        		asteroidInstances.remove(i);
        		asteroidPositions.remove(i);
        		asteroidFallSpeeds.removeIndex(i);
        		--asteroidsCount;
        	}
        }
    }

    @Override
    public void render() {
    	float dt = Gdx.graphics.getDeltaTime();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        
        camera.update();
        UpdateAsteroids(dt);
        
        modelBatch.begin(camera);
        modelBatch.render(backDropInstance, environment);
        
        for(int i = 0; i < asteroidsCount; ++i){
        	asteroidInstances.get(i).transform.rotate(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1);
        	modelBatch.render(asteroidInstances.get(i), environment);
        }
        
        if( ! gameOver ){
        	modelBatch.render(voltronInstance, environment);
            modelBatch.render(leftArmInstance, environment);
            modelBatch.render(rightArmInstance, environment);
        }
        modelBatch.end();
        
    	batch.begin();
    	for(ParticleEffect effect : explosionsEffects) {
			effect.draw(batch, dt);
			if(effect.isComplete()) {
				explosionsEffects.removeValue(effect, true);
				explosionEffectPool.free((PooledEffect)effect);
			}
		}
    	batch.end();
    	
    	CheckCollisions();
    }
    
    private void CheckCollisions(){
    	Vector3 ourPosition = new Vector3(bodyXPosition, bodyYPosition, bodyZPosition);
		ourPosition = camera.project(ourPosition);
		
    	for(int i = 0; i < asteroidsCount; ++i){
    		Vector3 position = asteroidPositions.get(i).cpy();
    		position = camera.project(position);
    		
    		float distance = ourPosition.dst2(position);
    		
    		if(distance < 600f && ! gameOver){
    			gameOver = true;
    			
    			ParticleEffect effect = explosionEffectPool.obtain();
            	effect.setPosition(ourPosition.x, ourPosition.y);
            	explosionsEffects.add(effect);
    		}
    	}
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
    
    public void MoveBody(float delta){
    	if( bodyXPosition + delta < -SideScrollLimit || bodyXPosition + delta > SideScrollLimit )
    		return;
    	
    	bodyXPosition += delta;
    	
    	voltronInstance.transform.setTranslation(bodyXPosition, bodyYPosition, bodyZPosition);
    	leftArmInstance.transform.setTranslation(bodyXPosition + ArmsOffset, bodyYPosition, bodyZPosition);
    	rightArmInstance.transform.setTranslation(bodyXPosition - ArmsOffset, bodyYPosition, bodyZPosition);
    }
    
    public void MoveLeftArm(float delta){
    	if( leftArmRotation + delta < ArmsStartRotation-ArmRotationLimit || 
    		leftArmRotation + delta > ArmsStartRotation+ArmRotationLimit)
    		return;
    	
    	leftArmRotation -= delta;
    	
    	leftArmInstance.transform.setToRotation(0, 0, 1, leftArmRotation);
    	leftArmInstance.transform.setTranslation(bodyXPosition + ArmsOffset, bodyYPosition, bodyZPosition);
    }
    
    public void MoveRightArm(float delta){
    	if( rightArmRotation + delta < ArmsStartRotation-ArmRotationLimit || 
    		rightArmRotation + delta > ArmsStartRotation+ArmRotationLimit)
    		return;
    	
    	rightArmRotation += delta;
    	
    	rightArmInstance.transform.setToRotation(0, 0, 1, -rightArmRotation);
    	rightArmInstance.transform.setTranslation(bodyXPosition - ArmsOffset, bodyYPosition, bodyZPosition);
    }

    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Keys.A)
        	MoveLeftArm(-3f);
        if(keycode == Keys.D)
        	SpawnAsteroid(random.nextFloat(), random.nextFloat(), 50f);
        
        /*
        if(keycode == Keys.C){        	
        	ParticleEffect effect = explosionEffectPool.obtain();
        	
        	Vector3 pos = asteroidPositions.get(random.nextInt(AsteroidsCount));
    		pos = camera.project(pos);
        	effect.setPosition(pos.x, pos.y);
        	
        	explosionsEffects.add(effect);
        }
        */
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
		oldX = -1;
		oldY = -1;
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
	public void OnTouchDrag(int screenX, int screenY, int pointer) {
		if(pointer > 0)
			return;
		
		if( oldX < 0){
			oldX = screenX;
			oldY = screenY;
			return;
		}
		
		float deltaX = (screenX - oldX) / 65.0f;
		float deltaY = (screenY - oldY) / 65.0f;
		
		oldX = screenX;
		oldY = screenY;
		
		if(deltaX > 20 || deltaY > 20)
			return;
		
		if(screenY > Gdx.graphics.getHeight() / 2){
			MoveBody(deltaX);
		}else if(screenX > Gdx.graphics.getWidth() / 2){
			MoveLeftArm(deltaX * 2.5f);
		}else{
			MoveRightArm(deltaX * 2.5f);
		}
		
        return;
	}
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		OnTouchDrag(screenX, screenY, pointer);
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