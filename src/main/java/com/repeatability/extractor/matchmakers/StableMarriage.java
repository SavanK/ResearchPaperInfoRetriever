package com.repeatability.extractor.matchmakers;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by savan on 6/10/17.
 */
public class StableMarriage {
    private int N, engagedCount;
    private String[][] menPref;
    private String[][] womenPref;
    private String[] men;
    private String[] women;
    private String[] womenPartner;
    private boolean[] menEngaged;

    public StableMarriage(String[] m, String[] w, String[][] mp, String[][] wp) {
        N = mp.length;
        engagedCount = 0;
        men = m;
        women = w;
        menPref = mp;
        womenPref = wp;
        menEngaged = new boolean[N];
        womenPartner = new String[N];
    }

    public void calculateMatches() {
        while (engagedCount < N) {
            int free;

            for (free = 0; free < N; free++)
                if (!menEngaged[free])
                    break;

            for (int i = 0; i < N && !menEngaged[free]; i++) {
                int index = womenIndexOf(menPref[free][i]);

                if (womenPartner[index] == null) {
                    womenPartner[index] = men[free];
                    menEngaged[free] = true;
                    engagedCount++;
                }
                else {
                    String currentPartner = womenPartner[index];

                    if (betterMatch(currentPartner, men[free], index)) {
                        womenPartner[index] = men[free];
                        menEngaged[free] = true;
                        menEngaged[menIndexOf(currentPartner)] = false;
                    }
                }
            }
        }
    }

    private boolean betterMatch(String curPartner, String newPartner, int index) {
        for (int i = 0; i < N; i++) {
            if (womenPref[index][i].equals(newPartner))
                return true;
            if (womenPref[index][i].equals(curPartner))
                return false;
        }
        return false;
    }

    private int menIndexOf(String str) {
        for (int i = 0; i < N; i++)
            if (men[i].compareToIgnoreCase(str) == 0)
                return i;
        return -1;
    }

    private int womenIndexOf(String str) {
        for (int i = 0; i < N; i++)
            if (women[i].compareToIgnoreCase(str) == 0)
                return i;
        return -1;
    }

    public Map<String, String> getCouples() {
        Map<String, String> couples = new HashMap<String, String>();
        for (int i = 0; i < N; i++) {
            couples.put(womenPartner[i], women[i]);
        }
        return couples;
    }
}
