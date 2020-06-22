package jndev.volleyball.court;

import jndev.volleyball.config.ConfigManager;
import jndev.volleyball.config.ConfigType;
import jndev.volleyball.config.Configs;

/**
 * class dedicated to loading and saving courts from and to file
 */
public class CourtManager {
    
    /**
     * court config instance
     */
    private static ConfigManager courtConfig = Configs.getConfig(ConfigType.COURT);
    
    
    /**
     * load all courts in file
     */
    public static void loadAll() {
        for (String name : courtConfig.getConfig().getKeys(false)) {
            loadCourt(name);
        }
    }
    
    /**
     * load one court by name
     *
     * @param name name of court to load
     */
    private static void loadCourt(String name) {
        Court court = new Court(name);
        court.setAnimations(courtConfig.getConfig().getBoolean(name + ".animations"));
        court.setEnabled(courtConfig.getConfig().getBoolean(name + ".enabled"));
        court.setSpeed(courtConfig.getConfig().getDouble(name + ".speed"));
        court.setTexture(courtConfig.getConfig().getString(name + ".texture"));
        if (courtConfig.getConfig().get(name + ".bounds") != null) {
            double[][] bounds = new double[2][3];
            bounds[0][0] = courtConfig.getConfig().getDouble(name + ".court.min.x");
            bounds[0][1] = courtConfig.getConfig().getDouble(name + ".court.min.y");
            bounds[0][2] = courtConfig.getConfig().getDouble(name + ".court.min.z");
            bounds[1][0] = courtConfig.getConfig().getDouble(name + ".court.max.x");
            bounds[1][1] = courtConfig.getConfig().getDouble(name + ".court.max.y");
            bounds[1][2] = courtConfig.getConfig().getDouble(name + ".court.max.z");
            court.setBounds(bounds);
        }
        if (courtConfig.getConfig().get(name + ".net") != null) {
            double[][] net = new double[2][3];
            net[0][0] = courtConfig.getConfig().getDouble(name + ".net.min.x");
            net[0][1] = courtConfig.getConfig().getDouble(name + ".net.min.y");
            net[0][2] = courtConfig.getConfig().getDouble(name + ".net.min.z");
            net[1][0] = courtConfig.getConfig().getDouble(name + ".net.max.x");
            net[1][1] = courtConfig.getConfig().getDouble(name + ".net.max.y");
            net[1][2] = courtConfig.getConfig().getDouble(name + ".net.max.z");
            court.setNet(net);
        }
    }
    
    /**
     * save all courts to file
     */
    public static void saveAll() {
        for(Court court : Court.getCourts().values()) {
            saveCourt(court);
        }
    }
    
    /**
     * save a court to file
     *
     * @param court court to save
     */
    private static void saveCourt(Court court) {
        String name = court.getName();
        
        courtConfig.getConfig().set(name + ".animations", court.hasAnimations());
        courtConfig.getConfig().set(name + ".enabled", court.isEnabled());
        courtConfig.getConfig().set(name + ".speed", court.getSpeed());
        courtConfig.getConfig().set(name + ".texture", court.getTexture());
        if (court.getBounds() != null) {
            double[][] bounds = court.getBounds();
            courtConfig.getConfig().set(name + ".court.min.x", bounds[0][0]);
            courtConfig.getConfig().set(name + ".court.min.y", bounds[0][1]);
            courtConfig.getConfig().set(name + ".court.min.z", bounds[0][2]);
            courtConfig.getConfig().set(name + ".court.max.x", bounds[1][0]);
            courtConfig.getConfig().set(name + ".court.max.y", bounds[1][1]);
            courtConfig.getConfig().set(name + ".court.max.z", bounds[1][2]);
        }
        if (court.getNet() != null) {
            double[][] net = court.getNet();
            courtConfig.getConfig().set(name + ".net.min.x", net[0][0]);
            courtConfig.getConfig().set(name + ".net.min.y", net[0][1]);
            courtConfig.getConfig().set(name + ".net.min.z", net[0][2]);
            courtConfig.getConfig().set(name + ".net.max.x", net[1][0]);
            courtConfig.getConfig().set(name + ".net.max.y", net[1][1]);
            courtConfig.getConfig().set(name + ".net.max.z", net[1][2]);
        }
        
        courtConfig.saveConfig();
        
    }
    
}