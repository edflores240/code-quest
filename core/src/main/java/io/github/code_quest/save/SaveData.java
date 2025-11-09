package io.github.code_quest.save;

import com.badlogic.gdx.utils.Array;
import java.util.Objects;

/**
 * Data transfer object representing the player's progress that can be serialized to JSON.
 */
public class SaveData {

    private float positionX;
    private float positionY;
    private int currentLevel;
    private int coins;
    private Array<String> inventory;
    private String characterKey;

    public SaveData() {
        this.inventory = new Array<>();
        this.characterKey = "male";
    }

    public SaveData(float positionX,
                    float positionY,
                    int currentLevel,
                    int coins,
                    Array<String> inventory,
                    String characterKey) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.currentLevel = currentLevel;
        this.coins = coins;
        this.inventory = inventory != null ? new Array<>(inventory) : new Array<>();
        this.characterKey = characterKey != null ? characterKey : "male";
    }

    public float getPositionX() {
        return positionX;
    }

    public void setPositionX(float positionX) {
        this.positionX = positionX;
    }

    public float getPositionY() {
        return positionY;
    }

    public void setPositionY(float positionY) {
        this.positionY = positionY;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public Array<String> getInventory() {
        return inventory;
    }

    public void setInventory(Array<String> inventory) {
        this.inventory = inventory != null ? new Array<>(inventory) : new Array<>();
    }

    public String getCharacterKey() {
        return characterKey;
    }

    public void setCharacterKey(String characterKey) {
        this.characterKey = characterKey != null ? characterKey : "male";
    }

    @Override
    public String toString() {
        return "SaveData{" +
                "positionX=" + positionX +
                ", positionY=" + positionY +
                ", currentLevel=" + currentLevel +
                ", coins=" + coins +
                ", inventory=" + inventory +
                ", characterKey='" + characterKey + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaveData saveData = (SaveData) o;
        return Float.compare(saveData.positionX, positionX) == 0 &&
                Float.compare(saveData.positionY, positionY) == 0 &&
                currentLevel == saveData.currentLevel &&
                coins == saveData.coins &&
                Objects.equals(inventory, saveData.inventory) &&
                Objects.equals(characterKey, saveData.characterKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positionX, positionY, currentLevel, coins, inventory, characterKey);
    }
}
