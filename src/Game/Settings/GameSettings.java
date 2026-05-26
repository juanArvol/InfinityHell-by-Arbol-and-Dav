package Game.Settings;

public class GameSettings {

    private static final GameSettings instance = new GameSettings();

    private boolean debugEnabled = false;

    private GameSettings(){}

    public static GameSettings getInstance() {
        return instance;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean value) {
        this.debugEnabled = value;
    }
}