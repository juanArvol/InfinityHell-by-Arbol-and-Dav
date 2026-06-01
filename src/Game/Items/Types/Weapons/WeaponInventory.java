package Game.Items.Types.Weapons;

import java.util.ArrayList;
import java.util.List;

public class WeaponInventory {

    private final List<WeaponSelected> weapons = new ArrayList<>();
    private int currentIndex = 0;

    public void addWeapon(WeaponSelected weapon) {
        weapons.add(weapon);
    }

    public WeaponSelected getCurrentWeapon() {
        if (weapons.isEmpty()) return null;
        return weapons.get(currentIndex);
    }

    public void nextWeapon() {
        if (weapons.isEmpty()) return;
        currentIndex = (currentIndex + 1) % weapons.size();
    }

    public void previousWeapon() {
        if (weapons.isEmpty()) return;
        currentIndex = (currentIndex - 1 + weapons.size()) % weapons.size();
    }
} 