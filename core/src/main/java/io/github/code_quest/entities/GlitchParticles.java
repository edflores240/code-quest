package io.github.code_quest.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;

public class GlitchParticles extends Actor {
    private static class Bit {
        float x, y, vx, vy, a, size;
    }

    private final Array<Bit> bits = new Array<>();
    private final Texture pixel;
    private final float width, height;
    private final Color tint = new Color();

    public GlitchParticles(float width, float height, int count, Color tint) {
        this.width = width; this.height = height;
        this.tint.set(tint);
        setBounds(0, 0, width, height);

        // 1x1 white pixel for quads
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.drawPixel(0, 0);
        this.pixel = new Texture(pm);
        pm.dispose();

        for (int i = 0; i < count; i++) bits.add(newBit());
    }

    private Bit newBit() {
        Bit b = new Bit();
        b.x = MathUtils.random(width);
        b.y = MathUtils.random(height);
        b.vx = MathUtils.random(-10f, 10f);
        b.vy = MathUtils.random(5f, 20f);
        b.a = MathUtils.random(0.06f, 0.2f);
        b.size = MathUtils.random(1f, 3f);
        return b;
    }

    @Override
    public void act(float delta) {
        for (int i = 0; i < bits.size; i++) {
            Bit b = bits.get(i);
            b.x += b.vx * delta;
            b.y += b.vy * delta;
            if (b.y > height + 5 || b.x < -5 || b.x > width + 5) bits.set(i, newBit());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color old = batch.getColor();
        for (Bit b : bits) {
            batch.setColor(tint.r, tint.g, tint.b, b.a * parentAlpha);
            batch.draw(pixel, b.x, b.y, b.size, b.size);
        }
        batch.setColor(old);
    }

    public void dispose() {
        pixel.dispose();
    }
}