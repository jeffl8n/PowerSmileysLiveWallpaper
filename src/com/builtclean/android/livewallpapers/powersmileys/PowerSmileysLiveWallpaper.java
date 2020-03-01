package com.builtclean.android.livewallpapers.powersmileys;

import java.util.ArrayList;
import java.util.Random;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnAreaTouchListener;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.Scene.ITouchArea;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.extension.ui.livewallpaper.BaseLiveWallpaperService;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class PowerSmileysLiveWallpaper extends BaseLiveWallpaperService
		implements SharedPreferences.OnSharedPreferenceChangeListener,
		IAccelerometerListener, IOnSceneTouchListener, IOnAreaTouchListener {

	public static final String SHARED_PREFS_NAME = "PowerSmileysLiveWallpaperSettings";

	private static float mCameraWidth = 480;
	private static float mCameraHeight = 720;

	private Display display;

	private Camera mCamera;

	private ScreenOrientation mScreenOrientation;
	private ScreenOrientation mInitOrientation;

	private Texture mTexture;

	private TiledTextureRegion mBoxSmileyTextureRegion;

	private Random mRnd = new Random();
	private ArrayList<AnimatedSprite> smileyArray = new ArrayList<AnimatedSprite>();
	private int powerLevel;

	private PhysicsWorld mPhysicsWorld;

	private float mGravityX;
	private float mGravityY;

	private Shape roof;
	private Shape right;
	private Shape ground;
	private Shape left;

	private final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(
			0, 0.5f, 0.5f);

	private final Vector2 mTempVector = new Vector2();

	private SharedPreferences mSharedPreferences;
	private int smileyColor;
	private int backgroundColor;

	private int lowLevel;
	private int mediumLevel;
	private int highLevel;

	private String currentBatteryLevel = "";
	private boolean smileyColorChanged = true;

	private BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int level = -1;
			if (rawlevel >= 0 && scale > 0) {
				level = (rawlevel * 100) / scale;
			}
			powerLevel = level;

			setSmileyColor();

			getEngine().runOnUpdateThread(new Runnable() {

				@Override
				public void run() {
					updateSmileys();
				}
			});
		}
	};

	@Override
	public org.anddev.andengine.engine.Engine onLoadEngine() {

		display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();

		mCameraWidth = display.getWidth();
		mCameraHeight = display.getHeight();

		RatioResolutionPolicy ratio;

		int rotation = display.getRotation();
		if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
			mScreenOrientation = ScreenOrientation.LANDSCAPE;
			mCamera = new Camera(0, 0, mCameraWidth, mCameraHeight);
			ratio = new RatioResolutionPolicy(mCameraHeight, mCameraWidth);
		} else {
			mScreenOrientation = ScreenOrientation.PORTRAIT;
			mCamera = new Camera(0, 0, mCameraWidth, mCameraHeight);
			ratio = new RatioResolutionPolicy(mCameraWidth, mCameraHeight);
		}
		mInitOrientation = mScreenOrientation;

		final EngineOptions engineOptions = new EngineOptions(true,
				mScreenOrientation, ratio, mCamera);
		engineOptions.getTouchOptions().setRunOnUpdateThread(true);

		return new org.anddev.andengine.engine.Engine(engineOptions);
	}

	private void setSmileyColor() {

		lowLevel = mSharedPreferences
				.getInt(PowerSmileysLiveWallpaperSettings.LOW_BATTERY_LEVEL_PREFERENCE_KEY,
						PowerSmileysLiveWallpaperSettings.LOW_BATTERY_LEVEL_DEFAULT);
		mediumLevel = mSharedPreferences
				.getInt(PowerSmileysLiveWallpaperSettings.MEDIUM_BATTERY_LEVEL_PREFERENCE_KEY,
						PowerSmileysLiveWallpaperSettings.MEDIUM_BATTERY_LEVEL_DEFAULT);
		highLevel = mSharedPreferences
				.getInt(PowerSmileysLiveWallpaperSettings.HIGH_BATTERY_LEVEL_PREFERENCE_KEY,
						PowerSmileysLiveWallpaperSettings.HIGH_BATTERY_LEVEL_DEFAULT);

		if (powerLevel >= highLevel) {
			smileyColor = mSharedPreferences
					.getInt(PowerSmileysLiveWallpaperSettings.FULL_SMILEY_COLOR_PREFERENCE_KEY,
							PowerSmileysLiveWallpaperSettings.FULL_SMILEY_COLOR_DEFAULT);

			if (currentBatteryLevel != PowerSmileysLiveWallpaperSettings.FULL_SMILEY_COLOR_PREFERENCE_KEY) {
				smileyColorChanged = true;
				currentBatteryLevel = PowerSmileysLiveWallpaperSettings.FULL_SMILEY_COLOR_PREFERENCE_KEY;
			}

		} else if (powerLevel >= mediumLevel) {
			smileyColor = mSharedPreferences
					.getInt(PowerSmileysLiveWallpaperSettings.HALF_SMILEY_COLOR_PREFERENCE_KEY,
							PowerSmileysLiveWallpaperSettings.HALF_SMILEY_COLOR_DEFAULT);

			if (currentBatteryLevel != PowerSmileysLiveWallpaperSettings.HALF_SMILEY_COLOR_PREFERENCE_KEY) {
				smileyColorChanged = true;
				currentBatteryLevel = PowerSmileysLiveWallpaperSettings.HALF_SMILEY_COLOR_PREFERENCE_KEY;
			}

		} else if (powerLevel >= lowLevel) {
			smileyColor = mSharedPreferences
					.getInt(PowerSmileysLiveWallpaperSettings.QUARTER_SMILEY_COLOR_PREFERENCE_KEY,
							PowerSmileysLiveWallpaperSettings.QUARTER_SMILEY_COLOR_DEFAULT);

			if (currentBatteryLevel != PowerSmileysLiveWallpaperSettings.QUARTER_SMILEY_COLOR_PREFERENCE_KEY) {
				smileyColorChanged = true;
				currentBatteryLevel = PowerSmileysLiveWallpaperSettings.QUARTER_SMILEY_COLOR_PREFERENCE_KEY;
			}

		} else {
			smileyColor = mSharedPreferences
					.getInt(PowerSmileysLiveWallpaperSettings.EMPTY_SMILEY_COLOR_PREFERENCE_KEY,
							PowerSmileysLiveWallpaperSettings.EMPTY_SMILEY_COLOR_DEFAULT);

			if (currentBatteryLevel != PowerSmileysLiveWallpaperSettings.EMPTY_SMILEY_COLOR_PREFERENCE_KEY) {
				smileyColorChanged = true;
				currentBatteryLevel = PowerSmileysLiveWallpaperSettings.EMPTY_SMILEY_COLOR_PREFERENCE_KEY;
			}

		}

		if (smileyColorChanged) {
			int size = smileyArray.size();
			for (int i = 0; i < size; i++) {
				AnimatedSprite smiley = smileyArray.get(i);

				smiley.setColor(
						(float) ((float) Color.red(smileyColor) / 255f),
						(float) ((float) Color.green(smileyColor) / 255f),
						(float) ((float) Color.blue(smileyColor) / 255f));

				if (powerLevel >= highLevel) {
					smiley.setCurrentTileIndex(0);
				} else if (powerLevel >= mediumLevel) {
					smiley.setCurrentTileIndex(1);
				} else if (powerLevel >= lowLevel) {
					smiley.setCurrentTileIndex(2);
				} else {
					smiley.setCurrentTileIndex(3);
				}

				smileyArray.set(i, smiley);
			}
			smileyColorChanged = false;
		}

	}

	@Override
	public void onLoadResources() {

		mTexture = new Texture(256, 64, TextureOptions.DEFAULT);
		TextureRegionFactory.setAssetBasePath("gfx/");
		mBoxSmileyTextureRegion = TextureRegionFactory.createTiledFromAsset(
				mTexture, this, "smileys_tiled.png", 0, 0, 4, 1);
		getEngine().getTextureManager().loadTexture(mTexture);

		enableAccelerometerSensor(this);

		mSharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, 0);
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * @see org.anddev.andengine.ui.IGameInterface#onLoadScene()
	 */
	@Override
	public Scene onLoadScene() {

		getEngine().registerUpdateHandler(new FPSLogger());

		final Scene scene = new Scene(2);
		backgroundColor = mSharedPreferences
				.getInt(PowerSmileysLiveWallpaperSettings.BACKGROUND_COLOR_PREFERENCE_KEY,
						PowerSmileysLiveWallpaperSettings.BACKGROUND_COLOR_DEFAULT);

		scene.setBackground(new ColorBackground((float) ((float) Color
				.red(backgroundColor) / 255f), (float) ((float) Color
				.green(backgroundColor) / 255f), (float) ((float) Color
				.blue(backgroundColor) / 255f)));
		scene.setOnSceneTouchListener(this);

		mPhysicsWorld = new PhysicsWorld(new Vector2(0,
				SensorManager.GRAVITY_EARTH), false);

		roof = new Rectangle(0, 0, 0, mCameraHeight);
		right = new Rectangle(0, 0, mCameraWidth, 0);
		ground = new Rectangle(mCameraWidth, 0, 0, mCameraHeight);
		left = new Rectangle(0, mCameraHeight, mCameraWidth, 0);

		PhysicsFactory.createBoxBody(mPhysicsWorld, roof, BodyType.StaticBody,
				wallFixtureDef);
		PhysicsFactory.createBoxBody(mPhysicsWorld, right, BodyType.StaticBody,
				wallFixtureDef);
		PhysicsFactory.createBoxBody(mPhysicsWorld, ground,
				BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(mPhysicsWorld, left, BodyType.StaticBody,
				wallFixtureDef);

		scene.getFirstChild().attachChild(roof);
		scene.getFirstChild().attachChild(right);
		scene.getFirstChild().attachChild(ground);
		scene.getFirstChild().attachChild(left);

		scene.registerUpdateHandler(mPhysicsWorld);

		scene.setOnAreaTouchListener(this);

		return scene;
	}

	@Override
	public void onLoadComplete() {

		setSmileyColor();

		getEngine().runOnUpdateThread(new Runnable() {
			@Override
			public void run() {
				updateSmileys();
			}
		});

		registerReceiver(batteryLevelReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));

	}

	@Override
	public void onDestroy() {

		getEngine().runOnUpdateThread(new Runnable() {

			@Override
			public void run() {
				int size = smileyArray.size();
				for (int i = 0; i < size; i++) {
					removeSmiley();
				}
			}
		});

		getEngine().getScene().reset();

		super.onDestroy();

	}

	@Override
	public void onAccelerometerChanged(
			final AccelerometerData pAccelerometerData) {

		if (display.getRotation() == Surface.ROTATION_0) {
			mGravityX = -pAccelerometerData.getX();
			mGravityY = pAccelerometerData.getY();
		} else if (display.getRotation() == Surface.ROTATION_90) {
			mGravityX = pAccelerometerData.getY();
			mGravityY = pAccelerometerData.getX();
		} else if (display.getRotation() == Surface.ROTATION_180) {
			mGravityX = pAccelerometerData.getX();
			mGravityY = -pAccelerometerData.getY();
		} else if (display.getRotation() == Surface.ROTATION_270) {
			mGravityX = -pAccelerometerData.getY();
			mGravityY = -pAccelerometerData.getX();
		}

		mTempVector.set(mGravityX, mGravityY);

		mPhysicsWorld.setGravity(mTempVector);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		backgroundColor = sharedPreferences
				.getInt(PowerSmileysLiveWallpaperSettings.BACKGROUND_COLOR_PREFERENCE_KEY,
						PowerSmileysLiveWallpaperSettings.BACKGROUND_COLOR_DEFAULT);

		final Scene scene = getEngine().getScene();
		scene.setBackground(new ColorBackground((float) ((float) Color
				.red(backgroundColor) / 255f), (float) ((float) Color
				.green(backgroundColor) / 255f), (float) ((float) Color
				.blue(backgroundColor) / 255f)));

		setSmileyColor();

	}

	private void addSmiley(final float pX, final float pY, int r, int g, int b) {

		final Scene scene = getEngine().getScene();

		final AnimatedSprite smiley;
		final Body body;

		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1,
				0.5f, 0.5f);

		smiley = new AnimatedSprite(pX, pY, mBoxSmileyTextureRegion);
		float scale = (float) Math.sqrt((mCameraWidth * mCameraHeight)
				/ (smiley.getWidth() * smiley.getHeight() * 100)) - 0.03f;
		smiley.setScale(scale);
		smiley.setRotation(mRnd.nextFloat());

		if (powerLevel >= highLevel) {
			smiley.setCurrentTileIndex(0);
		} else if (powerLevel >= mediumLevel) {
			smiley.setCurrentTileIndex(1);
		} else if (powerLevel >= lowLevel) {
			smiley.setCurrentTileIndex(2);
		} else {
			smiley.setCurrentTileIndex(3);
		}

		smiley.setColor((float) ((float) Color.red(smileyColor) / 255f),
				(float) ((float) Color.green(smileyColor) / 255f),
				(float) ((float) Color.blue(smileyColor) / 255f));

		body = PhysicsFactory.createCircleBody(mPhysicsWorld, smiley,
				BodyType.DynamicBody, objectFixtureDef);

		scene.registerTouchArea(smiley);
		scene.getLastChild().attachChild(smiley);
		mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(smiley,
				body, true, true));
		smileyArray.add(smiley);
	}

	private void updateSmileys() {

		int diff = powerLevel - smileyArray.size();

		// Add smileys if the power is more than the number of smileys.
		for (int i = 0; i < diff; i++) {
			PowerSmileysLiveWallpaper.this
					.addSmiley(
							mRnd.nextInt((int) PowerSmileysLiveWallpaper.mCameraWidth - 64),
							mRnd.nextInt((int) PowerSmileysLiveWallpaper.mCameraHeight - 64),
							Color.red(smileyColor), Color.green(smileyColor),
							Color.blue(smileyColor));
		}

		// Remove smileys if the power is less than the number of smileys.
		for (int i = diff; i < 0; i++) {
			removeSmiley();
		}
	}

	private void removeSmiley() {

		if (smileyArray.size() == 0)
			return;

		final AnimatedSprite smiley = smileyArray
				.remove(smileyArray.size() - 1);

		removeSmiley(smiley);

	}

	private void removeSmiley(AnimatedSprite smiley) {
		final Scene scene = getEngine().getScene();

		final PhysicsConnector smileyPhysicsConnector = mPhysicsWorld
				.getPhysicsConnectorManager().findPhysicsConnectorByShape(
						smiley);

		mPhysicsWorld.unregisterPhysicsConnector(smileyPhysicsConnector);
		mPhysicsWorld.destroyBody(smileyPhysicsConnector.getBody());

		scene.unregisterTouchArea(smiley);
		scene.getLastChild().detachChild(smiley);
	}

	@Override
	public boolean onSceneTouchEvent(Scene arg0, TouchEvent arg1) {
		return false;
	}

	@Override
	public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
			final ITouchArea pTouchArea, final float pTouchAreaLocalX,
			final float pTouchAreaLocalY) {

		if (pSceneTouchEvent.getAction() == MotionEvent.ACTION_DOWN) {
			mPhysicsWorld.postRunnable(new Runnable() {
				@Override
				public void run() {
					final AnimatedSprite smiley = (AnimatedSprite) pTouchArea;

					jumpSmiley(smiley);
				}
			});
		}
		return false;
	}

	private void jumpSmiley(final AnimatedSprite smiley) {

		final Body smileyBody = mPhysicsWorld.getPhysicsConnectorManager()
				.findBodyByShape(smiley);
		Vector2 impulse = new Vector2(mRnd.nextFloat(), mRnd.nextFloat());
		smileyBody.applyLinearImpulse(
				impulse,
				new Vector2(smileyBody.getWorldCenter().x, smileyBody
						.getWorldCenter().y));

		smileyBody.setLinearVelocity(mTempVector.set(mGravityX * -70, mGravityY
				* -70));
	}

	@Override
	public void onUnloadResources() {
	}

	@Override
	public void onGamePaused() {
	}

	@Override
	public void onGameResumed() {
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mInitOrientation == ScreenOrientation.PORTRAIT) {
			if (mScreenOrientation == ScreenOrientation.LANDSCAPE) {
				getEngine().getScene().setScaleX(mCameraWidth / mCameraHeight);
				getEngine().getScene().setScaleY(mCameraHeight / mCameraWidth);
			}
			if (mScreenOrientation == ScreenOrientation.PORTRAIT) {
				getEngine().getScene().setScale(1);
			}
		} else if (mInitOrientation == ScreenOrientation.LANDSCAPE) {
			if (mScreenOrientation == ScreenOrientation.PORTRAIT) {
				getEngine().getScene().setScale(1);
			}
			if (mScreenOrientation == ScreenOrientation.LANDSCAPE) {
				getEngine().getScene().setScaleX(mCameraWidth / mCameraHeight);
				getEngine().getScene().setScaleY(mCameraHeight / mCameraWidth);
			}
		}
	}

}