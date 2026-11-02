package dev.spaceclient.mods;

import java.util.Locale;

/** Controls and rendering features are independent of movable HUD widgets. */
public enum GameplayModule {
    TOGGLE_SPRINT("ToggleSprint", "AutoSprint or press-to-toggle sprint", "autosprint toggle sprint бег спринт"),
    FREE_LOOK("FreeLook", "Look around without turning your player", "freelook camera свободная камера обзор"),
    ZOOM("Zoom", "Hold a key for smooth camera zoom", "zoom приближение зум"),
    CROSSHAIR("Crosshair", "Customize shape, size, gap and color", "crosshair aim прицел"),
    FULLBRIGHT("Fullbright", "Brighten dark areas on your screen", "fullbright brightness gamma яркость свет гамма"),
    TIME_CHANGER("Time Changer", "Choose the sky's time of day", "time changer day night sunset sunrise время день ночь закат рассвет"),
    NO_HURT_CAM("No Hurt Cam", "Keep your camera steady when hurt", "no hurt cam damage shake tilt тряска камера урон качание"),
    SNAPLOOK("Snaplook", "Hold a key for a quick perspective view", "snaplook perspective third person взгляд вид третье лицо"),
    WEATHER_CHANGER("Weather Changer", "Choose clear skies, rain or snow", "weather changer clear rain snow погода дождь снег ясно"),
    CHAT_TIMESTAMPS("Chat Timestamps", "Show when new chat messages arrived", "chat timestamps time чат время сообщения"),
    DURABILITY_WARNING("Durability Warning", "Warn when equipped items are nearly broken", "durability warning alert armor tool прочность предупреждение броня инструмент"),
    BLOCK_OVERLAY("Block Overlay", "Color the outline and fill of your selected block", "block overlay outline fill rainbow блок обводка заливка радуга"),
    HIT_COLOR("Hit Color", "Customize the damage tint of visible entities", "hit color hurt damage tint цвет урон подсветка"),
    FOV_CONTROL("FOV Control", "Choose your view angle and dynamic FOV strength", "fov control field view sprint bow угол обзор камера лук");
    public final String title, description;
    private final String search;
    GameplayModule(String title, String description, String aliases) {
        this.title = title; this.description = description;
        search = (title + " " + description + " " + aliases).toLowerCase(Locale.ROOT);
    }
    public boolean matches(String query) { return search.contains(query.trim().toLowerCase(Locale.ROOT)); }
    public boolean enabled(ModSettings s) {
        switch (this) {
            case TOGGLE_SPRINT: return s.autoSprint(); case FREE_LOOK: return s.freeLook();
            case ZOOM: return s.zoom(); case CROSSHAIR: return s.crosshair();
            case FULLBRIGHT: return s.fullbright(); case TIME_CHANGER: return s.timeChanger();
            case NO_HURT_CAM: return s.noHurtCam();
            case SNAPLOOK: return s.snaplook(); case WEATHER_CHANGER: return s.weatherChanger();
            case CHAT_TIMESTAMPS: return s.chatTimestamps(); case DURABILITY_WARNING: return s.durabilityWarning();
            case BLOCK_OVERLAY: return s.blockOverlay(); case HIT_COLOR: return s.hitColor(); case FOV_CONTROL: return s.fovControl();
            default: throw new AssertionError(this);
        }
    }
    public boolean toggle(ModSettings s) {
        switch (this) {
            case TOGGLE_SPRINT: return s.toggleAutoSprint(); case FREE_LOOK: return s.toggleFreeLook();
            case ZOOM: return s.toggleZoom(); case CROSSHAIR: return s.toggleCrosshair();
            case FULLBRIGHT: return s.toggleFullbright(); case TIME_CHANGER: return s.toggleTimeChanger();
            case NO_HURT_CAM: return s.toggleNoHurtCam();
            case SNAPLOOK: return s.toggleSnaplook(); case WEATHER_CHANGER: return s.toggleWeatherChanger();
            case CHAT_TIMESTAMPS: return s.toggleChatTimestamps(); case DURABILITY_WARNING: return s.toggleDurabilityWarning();
            case BLOCK_OVERLAY: return s.toggleBlockOverlay(); case HIT_COLOR: return s.toggleHitColor(); case FOV_CONTROL: return s.toggleFovControl();
            default: throw new AssertionError(this);
        }
    }
}
