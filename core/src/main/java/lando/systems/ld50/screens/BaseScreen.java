package lando.systems.ld50.screens;

import aurelienribon.tweenengine.TweenManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import lando.systems.ld50.Config;
import lando.systems.ld50.Main;
import lando.systems.ld50.assets.Assets;
import lando.systems.ld50.audio.AudioManager;

public abstract class BaseScreen implements InputProcessor, ControllerListener, Disposable {

    private static final String TAG = BaseScreen.class.getSimpleName();

    public final Main game;
    public final Assets assets;
    public final TweenManager tween;
    public final SpriteBatch batch;
    public final Vector3 pointerPos;
    public Group settingsGroup;
    public VisWindow settingsWindow;
    public VisImage settingsPane;
    private Rectangle settingsPaneBoundsVisible;
    private Rectangle settingsPaneBoundsHidden;
    public boolean isSettingShown = false;
    public MoveToAction hideSettingsPaneAction;
    public MoveToAction showSettingsPaneAction;


    public boolean exitingScreen;
//    public Particles particles;
    public AudioManager audio;
    public OrthographicCamera worldCamera;
    public OrthographicCamera windowCamera;

    protected Stage uiStage;
    protected Skin skin;

    public BaseScreen() {
        this.game = Main.game;
        this.assets = game.assets;
        this.tween = game.tween;
        this.batch = assets.batch;
        this.audio = game.audio;
//        this.particles = new Particles(game.assets);
        this.pointerPos = new Vector3();

        this.exitingScreen = false;

        this.worldCamera = new OrthographicCamera();
        this.worldCamera.setToOrtho(false, Config.window_width, Config.window_height);
        this.worldCamera.update();

        this.windowCamera = new OrthographicCamera();
        this.windowCamera.setToOrtho(false, Config.window_width, Config.window_height);
        this.windowCamera.update();

        initializeUI();
    }

    /**
     * Override and call super to setup whatever ui is needed in a screen
     */
    protected void initializeUI() {
        // reset the stage in case it hasn't already been set to the current window camera orientation
        // NOTE - doesn't seem to be a way to directly set the stage camera as the window camera
        //  could go in the other direction, create the uiStage and set windowCam = stage.cam
        skin = VisUI.getSkin();
        StretchViewport viewport = new StretchViewport(windowCamera.viewportWidth, windowCamera.viewportHeight);
        uiStage = new Stage(viewport, batch);
        initializeSettingsUI();
    }

    //TODO: remove foloowing method
    private void TBD_initializeSettingsUITable() {
        VisTable rootTable = new VisTable();
        rootTable.setWidth(windowCamera.viewportWidth);
        rootTable.setHeight(windowCamera.viewportHeight);
        rootTable.align(Align.center);
        settingsWindow = new VisWindow("", "noborder");
        settingsWindow.align(Align.center | Align.top);
        settingsWindow.setColor(0f, 0f, 0f, .9f);
        settingsWindow.setWidth(windowCamera.viewportWidth / 2);
        settingsWindow.setHeight(windowCamera.viewportHeight);
        settingsWindow.padLeft(50f).padRight(50f);
        VisLabel settingLabel = new VisLabel("Settings", "outfit-medium-40px");
        settingsWindow.add(settingLabel).padBottom(40f);
        settingsWindow.row();
        VisLabel musicVolumeLabel = new VisLabel("Music Volume", "outfit-medium-20px");
        Drawable temp = skin.getDrawable("default-horizontal");

        settingsWindow.add(musicVolumeLabel).padBottom(10f);
        settingsWindow.row();
        VisSlider musicSlider = new VisSlider(0, 1f, .05f, false, "default-vertical");
        //musicSlider.setColor(0f, 0f, 0f, 1f);
        settingsWindow.add(musicSlider).width(windowCamera.viewportWidth / 2);
        settingsGroup = new Group();
        rootTable.add(settingsWindow).height(windowCamera.viewportHeight);
        settingsGroup.addActor(rootTable);
//        settingsGroup.addActor(settingsWindow);
        uiStage.addActor(settingsGroup);
    }

    private void initializeSettingsUI() {

        Window.WindowStyle defaultWindowStyle = skin.get("default", Window.WindowStyle.class);
        Window.WindowStyle glassWindowStyle = new Window.WindowStyle(defaultWindowStyle);
        glassWindowStyle.background = Assets.Patch.glass.drawable;


        settingsPaneBoundsVisible = new Rectangle(windowCamera.viewportWidth/4, 0, windowCamera.viewportWidth/2, windowCamera.viewportHeight);
        settingsPaneBoundsHidden = new Rectangle(settingsPaneBoundsVisible);
        settingsPaneBoundsHidden.y -= settingsPaneBoundsVisible.height;

        isSettingShown = false;
        Rectangle bounds = isSettingShown ? settingsPaneBoundsVisible : settingsPaneBoundsHidden;

//        settingsPane = new VisImage(Assets.Patch.glass_active.drawable);
//        settingsPane.setSize(bounds.width, bounds.height);
//        settingsPane.setPosition(bounds.x, bounds.y);
//        settingsPane.setColor(Color.DARK_GRAY);

        settingsWindow = new VisWindow("", glassWindowStyle);
        settingsWindow.setSize(bounds.width, bounds.height);
        settingsWindow.setPosition(bounds.x, bounds.y);
        settingsWindow.setColor(Color.DARK_GRAY);

        VisLabel settingLabel = new VisLabel("Settings", "outfit-medium-40px");
        settingsWindow.add(settingLabel).padBottom(40f);
        settingsWindow.row();
        settingsWindow.add(settingLabel).padBottom(40f);

//        settingsGroup = new Group();
//        settingsGroup.addActor(settingsPane);

        hideSettingsPaneAction = new MoveToAction();
        hideSettingsPaneAction.setPosition(settingsPaneBoundsHidden.x, settingsPaneBoundsHidden.y);
        hideSettingsPaneAction.setDuration(.5f);
        //hideSettingsPaneAction.setActor(settingsGroup);

        showSettingsPaneAction = new MoveToAction();
        showSettingsPaneAction.setPosition(settingsPaneBoundsVisible.x, settingsPaneBoundsVisible.y);
        showSettingsPaneAction.setDuration(.5f);
        //showSettingsPaneAction.setActor(settingsGroup);


        uiStage.addActor(settingsWindow);
    }

    public void transitionCompleted() {
        Controllers.clearListeners();
        Controllers.addListener(this);
        Gdx.input.setInputProcessor(this);
    }

    public void updateEvenIfPaused(float dt) {
        // ... add things here that need to update even when Time.pause() has been called
    }

    public void update(float dt) {
        worldCamera.update();
        windowCamera.update();
        uiStage.act(dt);

//        audio.update(dt);

        // toggle debug states
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            boolean wasShown = Config.debug_general;
            Config.debug_general = !Config.debug_general;

            Actor rootActor = uiStage.getRoot();
            Action transitionAction = (wasShown)
                    ? Actions.moveTo(0, -windowCamera.viewportHeight, 0.1f, Interpolation.exp10In)
                    : Actions.moveTo(0, 0, 0.2f, Interpolation.exp10Out);
            transitionAction.setActor(rootActor);
            uiStage.addAction(transitionAction);
        }
    }

    public void resize(int width, int height) {}
    public void renderFrameBuffers(SpriteBatch batch) {}

    public abstract void render(SpriteBatch batch);

    @Override
    public void dispose() {
        uiStage.dispose();
    }

    // ------------------------------------------------------------------------
    // InputProcessor default implementation (from InputAdapter)
    // ------------------------------------------------------------------------

    @Override
    public boolean keyDown (int keycode) {
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped (char character) {
        return false;
    }

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved (int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled (float amountX, float amountY) {
        return false;
    }

    // ------------------------------------------------------------------------
    // ControllerListener default implementation (from ControllerAdapter)
    // ------------------------------------------------------------------------

    @Override
    public boolean buttonDown (Controller controller, int buttonIndex) {
        if (Config.debug_general) {
            Gdx.app.log(TAG, "controller " + controller.getName() + " button " + buttonIndex + " down");
        }
        return false;
    }

    @Override
    public boolean buttonUp (Controller controller, int buttonIndex) {
        if (Config.debug_general) {
            Gdx.app.log(TAG, "controller " + controller.getName() + " button " + buttonIndex + " up");
        }
        return false;
    }

    @Override
    public boolean axisMoved (Controller controller, int axisIndex, float value) {
        float deadzone = 0.2f;
        if (Config.debug_general && Math.abs(value) > deadzone) {
            Gdx.app.log(TAG, "controller " + controller.getName() + " axis " + axisIndex + " moved " + value);
        }
        return false;
    }

    @Override
    public void connected (Controller controller) {
        Gdx.app.log(TAG, "controller connected: '" + controller.getName() + "' id:" + controller.getUniqueId());
    }

    @Override
    public void disconnected (Controller controller) {
        // TODO - pause game and wait for reconnect or confirmation?
        Gdx.app.log(TAG, "controller disconnected: '" + controller.getName() + "' id:" + controller.getUniqueId());
    }

}
