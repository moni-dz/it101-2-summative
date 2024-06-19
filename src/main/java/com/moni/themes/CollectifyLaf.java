// Lythe Marvin L. Lacre
package com.moni.themes;

import com.formdev.flatlaf.FlatLightLaf;

public class CollectifyLaf extends FlatLightLaf {
    public static boolean setup() {
        return setup(new CollectifyLaf());
    }

    @Override
    public String getName() {
        return "CollectifyLaf";
    }
}
