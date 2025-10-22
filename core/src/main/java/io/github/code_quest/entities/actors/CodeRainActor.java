package io.github.code_quest.entities.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;

// Lightweight "data rain" actor (no shaders) using font glyphs.
public class CodeRainActor extends Actor {
    private static class Drop {
        float x, y, speed, len;
        String content;
        float alpha;
    }

    private final Array<Drop> drops = new Array<>();
    private final BitmapFont font;
    private final Color color = new Color();
    private final float width, height;

    public CodeRainActor(float width, float height, BitmapFont font, Color tint) {
        this.width = width;
        this.height = height;
        this.font = font;
        setBounds(0, 0, width, height);
        for (int i = 0; i < 60; i++) drops.add(newDrop());
        this.color.set(tint);
    }

    private Drop newDrop() {
        Drop d = new Drop();
        d.x = MathUtils.random(0f, width);
        d.y = MathUtils.random(height, height * 2f);
        d.speed = MathUtils.random(60f, 120f);
        d.len = MathUtils.random(6f, 16f);
        d.alpha = MathUtils.random(0.08f, 0.22f);
        d.content = randomSnippet((int)d.len);
        return d;
    }

    private String randomSnippet(int len) {
        final char[] pool = "publicstaticvoidclassintfloatdoublebooleanbytecharStringreturnifelseforwhiletrycatchnewextendsimplementsnulltruefalse".toCharArray();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(pool[MathUtils.random(pool.length - 1)]);
        return sb.toString();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        for (int i = 0; i < drops.size; i++) {
            Drop d = drops.get(i);
            d.y -= d.speed * delta;
            if (d.y < -20f) {
                drops.set(i, newDrop());
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color old = batch.getColor();
        for (Drop d : drops) {
            batch.setColor(color.r, color.g, color.b, d.alpha * parentAlpha);
            font.draw(batch, d.content, d.x, d.y);
        }
        batch.setColor(old);
    }
}