package no.pepega;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Machine {
    Map<String, String> comps;
    byte[] eeprom;

    public Machine(byte[] eeprom) {
        comps = new HashMap<>();
        comps.put("0", "computer");
        //comps.put("1", "gpu");
        //comps.put("2", "screen");
        comps.put("3", "eeprom");

        this.eeprom = eeprom;
    }

    public Object[] invoke(String addr, String action, Object[] attr) {
        if (addr.equals("3") && action.equals("get"))
            return new Object[]{eeprom};
        return null;
    }

    public void beep(short hz, short len) {
        System.out.println(System.out.format("Beep for %dms at %dhz", len, hz));
    }

    public Map<String, String> components() {
        return comps;
    }

    public int componentCount() {
        return comps.size();
    }
}
