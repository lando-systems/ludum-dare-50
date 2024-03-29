package lando.systems.ld50.objects;

import aurelienribon.tweenengine.Tween;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import lando.systems.ld50.Main;
import lando.systems.ld50.assets.Assets;
import lando.systems.ld50.assets.ImageInfo;
import lando.systems.ld50.utils.accessors.Vector3Accessor;

public class AnimationDecal {

    private float time = 0;

    public boolean dead = false;
    public boolean saved = false;

    protected Landscape landscape;
    private ImageInfo imageInfo;

    private Vector3 initPos = new Vector3();
    protected Vector3 position = new Vector3();

    private Animation<TextureRegion> regionAnimation;
    private Animation<TextureRegion> waveAnimation;

    protected float moveTime, moveTimeTotal;
    private Vector3 movePosition = new Vector3();

    private float waveTime = 0;

    private float directionX;
    private boolean right = true;

    private Array<Decal> decals = new Array<>();
    private Array<Decal> waveDecals = new Array<>();

    public boolean autoMove = false;
    public boolean isPerson = false;

    // real shitty, but it's ld
    public boolean isPlow = false;
    public boolean isHeli = false;

//    public AnimationDecal(ImageInfo imageInfo, int x, int z) {
//        this(Main.game.getScreen().assets, imageInfo, ((GameScreen) Main.game.getScreen()).landscape, x, z);
//    }

    public AnimationDecal(Assets assets, ImageInfo imageInfo, Landscape landscape, int x, int y, int z) {
        regionAnimation = new Animation<>(0.1f, assets.atlas.findRegions(imageInfo.region), Animation.PlayMode.LOOP);
        waveAnimation = (imageInfo.waveRegion != null)
                ? new Animation<>(0.1f, assets.atlas.findRegions(imageInfo.waveRegion), Animation.PlayMode.LOOP_PINGPONG)
                : regionAnimation;

        this.landscape = landscape;
        this.imageInfo = imageInfo;

        TextureRegion r = regionAnimation.getKeyFrame(0);

        // 0 is center of image
        if (!setTilePosition(initPos, x, y, z)) {
            setTilePosition(initPos, 0, -1,  0);
        }
        position.set(initPos);

        directionX = imageInfo.right ? 1 : -1;

        addDecals(regionAnimation, decals);
        addDecals(waveAnimation, waveDecals);
    }

    private void addDecals(Animation<TextureRegion> animation, Array<Decal> decals) {
        for (TextureRegion region : animation.getKeyFrames()) {
            Decal decal = Decal.newDecal(region, true);
            decal.setDimensions(imageInfo.width, imageInfo.height);
            // decal.setPosition(position);
            decals.add(decal);
        }
    }

    private boolean setTilePosition(Vector3 pos, int x, int y, int z) {
        float ix = x + MathUtils.random();
        float iz = z + MathUtils.random();

        float iy = (y > -1) ? y : landscape.getHeightAt(ix, iz) +  imageInfo.height / 2;
        pos.set(ix, iy, iz);
        return iy >= 0;
    }

    public void flyAway(int x, int z) {
        movePosition.set(x, 20, z);
        moveTimeTotal = 2;
        moveTime = 0;
    }

    public void moveToTile(int x, int z) {
        if (setTilePosition(movePosition, x, -1, z)) {
            right = (movePosition.x > position.x);
            moveTimeTotal = Math.abs(movePosition.dst(initPos)) / 4;
            moveTime = 0;
        } else {
            movePosition.setZero();
        }
    }

    public void update(float dt) {
        if (!launched) {
            time += dt;
        }

        updateMovement(dt);
    }

    public void hit() {
        this.launch();
//        int triNum = 0;
//        float xpart = position.x - (int)position.x - 0.5f;
//        float zpart = position.z - (int)position.z - 0.5f;
//        if (xpart > Math.abs(zpart)) { triNum = 1; }
//        else if (xpart < -Math.abs(zpart)) { triNum = 3; }
//        else if (zpart > Math.abs(xpart)) { triNum = 2; }
//        else { triNum = 0; }
//        bloodPos.setZero();
//        Triangle k = landscape.tiles[(int)bloodPos.x + (int)bloodPos.z * Landscape.TILES_WIDE].getTriangles().get(triNum);
//        bloodPos.add(k.p1);
//        bloodPos.add(k.p2);
//        bloodPos.add(k.p3);
//        bloodPos.scl(1f/3);
        Decal d = Decal.newDecal(Main.game.assets.particles.blood, true);
//        t1Vec.set(k.getNormal());
//        t1Vec.scl(0.125f);
//        bloodPos.add(t1Vec);
//
//        //d.setPosition(bloodPos);
//
//        d.setPosition(this.position);
//        bloodUp.set(bloodPos);
//        t1Vec.scl(8f);
//        bloodPos.add(t1Vec);
//        bloodUp.sub(k.p1);
//        bloodUp.nor();
//        d.lookAt(bloodPos,bloodUp);
        d.setPosition(this.position.x, this.position.y - this.imageInfo.height + 0.1f, this.position.z);
        d.lookAt(this.position, Vector3.Y);
        d.setDimensions(0.4f, 0.4f);

        landscape.screen.decalsStatic.add(d);
    }

    Vector3 bloodUp = new Vector3();
    Vector3 bloodPos = new Vector3();
    Vector3 t1Vec = new Vector3();
    boolean wasInSnow = false;

    protected void updateMovement(float dt) {

        if (isInSnow() && !launched) {
            if (!wasInSnow) {
                wasInSnow = true;
                // move to center of tile
                float targetX = (float) Math.floor(position.x) + 0.4f;
                float targetZ = (float) Math.floor(position.z) + 0.4f;
                Tween.to(position, Vector3Accessor.XZ, 0.1f)
                        .target(targetX, targetZ)
                        .start(Main.game.tween);
            }
            waveTime += dt;
            return;
        }

        if (moveTimeTotal > 0) {
            moveTime += 0.4f * dt;
            float lerp = MathUtils.clamp(moveTime / moveTimeTotal, 0, 1);
            float x = MathUtils.lerp(initPos.x, movePosition.x, lerp);
            float z = MathUtils.lerp(initPos.z, movePosition.z, lerp);
            float y = (isHeli) ? MathUtils.lerp(initPos.y, movePosition.y, lerp)
                    : landscape.getHeightAt(x, z) +  imageInfo.height / 2;

            if (launched) {
                y = position.y + (dt * 2);
                x = position.x - dt;
                z = position.z + (dt * 2);
                launchTime += dt;
            }

            position.set(x, y, z);

            if (lerp == 1) {
                moveTimeTotal = 0;
                initPos.set(position);

                if (launched) {
                    dead = true;
                    return;
                }

                completeMovement();
            }
        }
    }

    protected void completeMovement() {
        if (autoMove) {
            moveToTile(landscape.getRandomX(), getNextZ());
        }
    }

    private int getNextZ() {
        int curZ = (int)initPos.z;

        int dif = MathUtils.random.nextInt(4);
        int z = curZ + ((MathUtils.random.nextBoolean()) ? dif : -dif);
        return MathUtils.clamp(z, 4, Landscape.TILES_LONG - 8);
    }

    protected boolean isInSnow() {
        // protecc
        int index = (int)position.x + (int)position.z * Landscape.TILES_WIDE;
        if (index >= landscape.tiles.length) {
            return false;
        }
        float snow =  landscape.tiles[index].getAverageSnowHeight();
        return snow > 0.08f;
    }

    public Decal get() {
        int index = (waveTime > 0) ? waveAnimation.getKeyFrameIndex(waveTime) : regionAnimation.getKeyFrameIndex(time);
        Decal decal = (waveTime > 0) ? waveDecals.get(index) : decals.get(index);
        decal.setScaleX(right ? directionX : -directionX);
        decal.setPosition(position);

        // NOTE: Decal.lookAt breaks this, so we do it externally after the lookAt gets called in GameScreen.update()/launchTime
        if (launchTime > 0) {
            decal.setRotationZ(launchTime * 540);
        }

        return decal;
    }

    public boolean launched = false;
    public float launchTime = 0;

    public void launch() {
        launched = true;
        waveTime = moveTime = 0;
        moveTimeTotal = 5;
        Main.game.audio.playSound(this.imageInfo.scream, 0.35F);
    }

    public boolean isOn(LandTile tile) {
        int x = tile.intX;
        int z = tile.intZ;
        return (position.x >= x && position.x <= x + tile.width && position.z >= z && position.z <= z + tile.width);
    }
}
