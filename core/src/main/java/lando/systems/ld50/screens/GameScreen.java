package lando.systems.ld50.screens;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Linear;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.UBJsonReader;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisWindow;
import lando.systems.ld50.Config;
import lando.systems.ld50.Main;
import lando.systems.ld50.cameras.SimpleCameraController;
import lando.systems.ld50.objects.Landscape;
import lando.systems.ld50.utils.accessors.PerspectiveCameraAccessor;
import lando.systems.ld50.utils.screenshake.ScreenShakeCameraController;
import text.formic.Stringf;

public class GameScreen extends BaseScreen {

    private static final String TAG = GameScreen.class.getSimpleName();

    public static class Stats {
        // TODO - add stats vars
    }

    private final Color background = Color.SKY.cpy();
    private final PerspectiveCamera camera;
    private final ScreenShakeCameraController shaker;
    private final SimpleCameraController cameraController;
    private final Landscape landscape;

    private final Environment env;
    private final ModelBatch modelBatch;

    private Model testModel;
    private Model coordsModel;
    private ModelInstance testInstance;
    private ModelInstance coords;

    private Vector3 touchPos;
    private Vector3 startPos, endPos;
    private Vector3 startUp, endUp;
    private Vector3 startDir, endDir;
    private Tween cameraTween;

    private Vector3 lightDir;
    public DirectionalLight light;

    public FrameBuffer fb;
    public Texture fbTex;
    public Pixmap pickPixmap;

    public GameScreen() {
        camera = new PerspectiveCamera(70f, Config.window_width, Config.window_height);
        initializeCamera();

        cameraController = new SimpleCameraController(camera);
        shaker = new ScreenShakeCameraController(camera);

        lightDir = new Vector3(-1f, -.8f, -.2f);

        landscape = new Landscape(this);

        modelBatch = new ModelBatch();
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        env.add(light = new DirectionalLight().set(0.8f, 0.8f, 0.8f, lightDir.x, lightDir.y, lightDir.z));

        loadModels();

        InputMultiplexer mux = new InputMultiplexer(uiStage, this, cameraController);
        Gdx.input.setInputProcessor(mux);
        fb =  new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        fbTex = fb.getColorBufferTexture();
        touchPos = new Vector3();
    }

    @Override
    public void dispose() {
        super.dispose();
        testModel.dispose();
        coordsModel.dispose();
        modelBatch.dispose();
    }

    @Override
    public void transitionCompleted() {
        super.transitionCompleted();
        InputMultiplexer mux = new InputMultiplexer(uiStage, this, cameraController);
        Gdx.input.setInputProcessor(mux);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        boolean gameOver = isGameOver();

        camera.update();
        cameraController.update();
        shaker.update(dt);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (cameraTween.isPaused()) {
                cameraTween.resume();
            } else {
                cameraTween.pause();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            dumpCameraVecsToLog();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            shaker.addDamage(0.5f);
            landscape.startAvalanche();
        }

        touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        worldCamera.unproject(touchPos);
        Vector2 tile = getSelectedTile((int)touchPos.x, (int)touchPos.y);
        landscape.setSelectedTile((int)tile.x, (int)tile.y);

        landscape.update(dt);

        if (pickPixmap != null){
            pickPixmap.dispose();
        }

        updateDebugElements();
    }

    @Override
    public void render(SpriteBatch batch) {
        ScreenUtils.clear(background, true);

        // draw world
        batch.setProjectionMatrix(shaker.getCombinedMatrix());
        batch.begin();
        {
            // TODO - shaker breaks camera controller rotation,
            //  might not be important if we maintain control of the camera throughout
//            landscape.render(batch, shaker.getViewCamera());
            landscape.render(batch, camera);
        }
        batch.end();

        // TODO - might need to make everything draw with the model batch
        modelBatch.begin(camera);
        {
            modelBatch.render(testInstance, env);
            modelBatch.render(coords, env);
            landscape.render(modelBatch, env);
        }
        modelBatch.end();

        // NOTE: always draw so the 'hide' transition is visible
        uiStage.draw();

        batch.setProjectionMatrix(windowCamera.combined);
        batch.begin();
        {
            // TODO -
            batch.draw(fbTex, 0, 100, 100, -100);
        }
        batch.end();
    }


    public void renderFrameBuffers(SpriteBatch batch) {
        fb.begin();
        ScreenUtils.clear(Color.CLEAR, true);
        landscape.renderFBO(camera);
        pickPixmap =  Pixmap.createFromFrameBuffer(0, 0, fb.getWidth(), fb.getHeight());

        fb.end();
    }

    public boolean isGameOver() {
        return false;
    }

    private void initializeCamera() {
        camera.near = 0.1f;
        camera.far = 1000f;

        // orient manually for the swank
        startPos = new Vector3(2f, 4f, 10f);
        endPos   = new Vector3(2f, 4f, 100f);
        startUp  = new Vector3(Vector3.Y.cpy());
        endUp    = new Vector3(Vector3.Y.cpy());
        startDir = new Vector3(0.50008386f,-0.656027f,-0.5652821f);
        endDir   = new Vector3(0.50008386f,-0.656027f,-0.5652821f);

        camera.position.set(startPos);
        camera.up.set(startUp);
        camera.direction.set(startDir);
        camera.update();

        // TEMPORARY, probably (until we can get some nicer spline based travel)
        cameraTween = Tween.to(camera, PerspectiveCameraAccessor.POS, 12f)
                .target(endPos.x, endPos.y, endPos.z)
                .ease(Linear.INOUT)
                .delay(3.5f)
                .repeatYoyo(-1, 2f)
                .start(game.tween);
    }

    private void loadModels() {
        BoundingBox box = new BoundingBox();
        G3dModelLoader loader = new G3dModelLoader(new UBJsonReader());
        testModel = loader.loadModel(Gdx.files.internal("models/house-2.g3db"));
        testInstance = new ModelInstance(testModel);
        testInstance.calculateBoundingBox(box);
        testInstance.transform
                .scale(
                        1f / (box.max.x - box.min.x),
                        1f / (box.max.y - box.min.y),
                        1f / (box.max.z - box.min.z))
                .setTranslation(0.5f, 0f, 0.5f)
        ;

        ModelBuilder builder = new ModelBuilder();
        coordsModel = builder.createXYZCoordinates(1f, 0.1f, 0.5f, 6, GL20.GL_TRIANGLES,
                new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked);
        coords = new ModelInstance(coordsModel);
        coords.transform.setToTranslation(0, 0, 0);
    }

    // ------------------------------------------------------------------------
    // InputAdapter overrides
    // ------------------------------------------------------------------------

    private final Vector3 touchStart = new Vector3();
    private final Vector3 touchDrag = new Vector3();

    @Override
    public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        if (isGameOver()) return false;
        // ...
        return super.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp (int screenX, int screenY, int pointer, int button) {
        if (isGameOver()) return false;
        // ...
        return super.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged (int screenX, int screenY, int pointer) {
        if (isGameOver()) return false;
        // ...
        return super.touchDragged(screenX, screenY, pointer);
    }

    // ------------------------------------------------------------------------
    // ControllerListener default implementation (from ControllerAdapter)
    // ------------------------------------------------------------------------

    @Override
    public boolean buttonUp(Controller controller, int buttonIndex) {
        return super.buttonUp(controller, buttonIndex);
    }

    @Override
    public boolean axisMoved(Controller controller, int axisIndex, float value) {
        return super.axisMoved(controller, axisIndex, value);
    }

    // ------------------------------------------------------------------------
    // Private implementation details
    // ------------------------------------------------------------------------

    Vector2 hoverPos = new Vector2();
    Color pickColor = new Color();
    public Vector2 getSelectedTile(int screenX, int screenY) {
        if (pickPixmap == null) return hoverPos.set(-1, -1);
        int x = screenX;
        int y = screenY;
        pickColor.set(pickPixmap.getPixel(x, y));
        if (pickColor.a == 0) return hoverPos.set(-1, -1);
        int col = (int) (pickColor.r * (255f));
        int row = (int) (pickColor.g * (255f));
        return hoverPos.set(col, row);
    }

    private static class DebugElements {
        public static VisLabel fpsLabel;
        public static VisLabel javaHeapLabel;
        public static VisLabel nativeHeapLabel;
        public static VisLabel drawCallLabel;
        public static VisLabel simTimeLabel;
        public static VisLabel gamepadAxisLabel;
        public static VisLabel cameraLabel;
    }

    @Override
    protected void initializeUI() {
        super.initializeUI();
        //TODO: remove before launch (or just keep hidden)
        initializeDebugUI();
    }

    private void initializeDebugUI() {
        VisWindow debugWindow = new VisWindow("", true);
        debugWindow.setFillParent(true);
        debugWindow.setColor(1f, 1f, 1f, 0.4f);
        debugWindow.align(Align.top);

        VisLabel label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.fpsLabel = label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.javaHeapLabel = label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.nativeHeapLabel = label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.drawCallLabel = label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.simTimeLabel = label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.gamepadAxisLabel = label;

        label = new VisLabel();
        debugWindow.add(label).growX().row();
        DebugElements.cameraLabel = label;

        uiStage.addActor(debugWindow);
    }

    private void updateDebugElements() {
        DebugElements.fpsLabel.setText(Config.getFpsString());
        DebugElements.javaHeapLabel.setText(Config.getJavaHeapString());
        DebugElements.nativeHeapLabel.setText(Config.getNativeHeapString());
        DebugElements.drawCallLabel.setText(Config.getDrawCallString(batch));
        DebugElements.cameraLabel.setText(
                Stringf.format("Camera: up%s dir%s side%s",
                camera.up, camera.direction, side.set(camera.up).crs(camera.direction).nor()
        ));
    }
    private final Vector3 side = new Vector3();

    private void dumpCameraVecsToLog() {
        Gdx.app.log(TAG, Stringf.format("Camera: pos%s up%s dir%s side%s",
                camera.position, camera.up, camera.direction,
                side.set(camera.up).crs(camera.direction).nor())); }

}
