package jeremynoesen.volleyball.ball;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import jeremynoesen.volleyball.VolleyBall;
import jeremynoesen.volleyball.court.Court;
import org.bukkit.*;
import org.bukkit.craftbukkit.libs.org.apache.commons.codec.binary.Base64;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ball creation class
 *
 * @author Jeremy Noesen
 */
public class Ball {
    
    /**
     * list of all alive ball entities
     */
    private static Set<Entity> balls = new HashSet<>();
    
    /**
     * armorstand to make ball physics and wear the head
     */
    private final ArmorStand ball;
    
    /**
     * whether the ball removal has been started or not
     */
    private boolean end;
    
    /**
     * player serving this ball
     */
    private final Player player;
    
    /**
     * whether the ball has gone over the net or not
     */
    private boolean volleyed;
    
    /**
     * number of times the ball has gone over the net
     */
    private int volleys;
    
    /**
     * court this ball is on
     */
    private final Court court;
    
    /**
     * Creates a new ball
     *
     * @param player player to create the ball at
     */
    public Ball(Player player) {
        this.player = player;
        this.end = false;
        this.volleyed = false;
        this.volleys = 0;
        this.court = Court.get(player);
        
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.75).setY(-0.5));
        loc.setPitch(0);
        loc.setYaw(0);
        
        this.ball = player.getLocation().getWorld().spawn(loc, ArmorStand.class);
        balls.add(ball);
        
        ball.setSmall(true);
        ball.setCollidable(false);
        ball.getWorld().playSound(ball.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2, 0);
        ball.setCustomNameVisible(false);
        ball.setSilent(true);
        ball.setInvulnerable(true);
        ball.setVisible(false);
        ball.setBasePlate(false);
        ball.setGravity(true);
        setTexture(court.getTexture());
    }
    
    /**
     * @param url link to the player skin to get the skull from
     */
    public void setTexture(String url) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (url.isEmpty())
            return;
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        Field profileField;
        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            e1.printStackTrace();
        }
        head.setItemMeta(headMeta);
        ball.getEquipment().setHelmet(head);
    }
    
    /**
     * serves the volleyball
     */
    public void serve() {
        court.setBall(this);
        boolean animated = court.hasAnimations();
        boolean restricted = court.hasRestrictions();
        
        ball.setVelocity(player.getLocation().getDirection().multiply(0.05).add(new Vector(0, 0.5 + (0.1 * court.getSpeed()), 0)));
        
        if (animated) {
            Location loc = player.getLocation();
            double radius = 0.5;
            for (double y = 0; y <= Math.PI * 2; y += 0.175) {
                double x = radius * Math.cos(y);
                double z = radius * Math.sin((Math.PI * 2) - y);
                player.getWorld().spawnParticle(Particle.TOTEM,
                        (float) (loc.getX() + x), (float) (loc.getY() + 2),
                        (float) (loc.getZ() + z), 0, 0, 0, 0, 1);
            }
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                
                ball.setFallDistance(0);
                
                if (court.isAboveNet(ball.getLocation()) && !volleyed) {
                    volleyed = true;
                    volleys++;
                    for (Player players : court.getPlayersOnCourt()) {
                        players.sendTitle("", ChatColor.WHITE + Integer.toString(volleys), 0, 10, 10);
                    }
                } else if (!court.isAboveNet(ball.getLocation())) {
                    volleyed = false;
                }
                
                if (animated) {
                    ball.getWorld().spawnParticle(Particle.CRIT,
                            ball.getLocation().add(new Vector(0, 0.75, 0)), 0, 0, 0, 0, 1);
                }
                
                if (restricted) {
                    Vector vec = ball.getVelocity();
                    Location loc = ball.getLocation();
                    if (loc.getBlock().getX() < court.getBounds()[0][0] || loc.getBlock().getX() > court.getBounds()[1][0])
                        ball.setVelocity(vec.setX(-vec.getX()));
                    if (loc.getBlock().getY() < court.getBounds()[0][1]) ball.setVelocity(vec.setY(-vec.getY()));
                    if (loc.getBlock().getY() > court.getBounds()[1][1])
                        ball.setVelocity(vec.setY(-vec.getY() * 1.25));
                    if (loc.getBlock().getZ() < court.getBounds()[0][2] || loc.getBlock().getZ() > court.getBounds()[1][2])
                        ball.setVelocity(vec.setZ(-vec.getZ()));
                }
                
                if (ball.isDead()) {
                    end = true;
                    this.cancel();
                }
                
                if (ball.isOnGround() || ball.getLocation().add(0, 0.5, 0).getBlock().getType() != Material.AIR) {
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ball.isOnGround() || ball.getLocation().add(0, 0.5, 0).getBlock().getType() != Material.AIR) {
                                if (!end) remove();
                            }
                        }
                    }.runTaskLater(VolleyBall.getInstance(), 3);
                }
            }
        }.runTaskTimer(VolleyBall.getInstance(), 0, 1);
    }
    
    /**
     * removes the volleyball with or without animations
     */
    public void remove() {
        boolean animated = court.hasAnimations();
        if (animated) {
            end = true;
            double radius = 1;
            Location loc = ball.getLocation();
            for (double y = 0; y <= 6.28; y += 1.04) {
                double finalY = y;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double x = (radius - 0.14 * finalY) * Math.cos(finalY);
                        double z = (radius - 0.14 * finalY) * Math.sin(finalY);
                        ball.getWorld().spawnParticle(Particle.CLOUD,
                                (float) (loc.getX() + x), (float) (loc.getY() + 0.3),
                                (float) (loc.getZ() + z), 0, 0, 0, 0, 1);
                        Location loc1 = loc;
                        loc1.setYaw((float) finalY * 20);
                        loc1.setY(loc1.subtract(0, 0.1 * finalY, 0).getY());
                        ball.teleport(loc1);
                    }
                }.runTaskLater(VolleyBall.getInstance(), (long) y);
            }
            for (double y = 0; y <= 6.28; y += 0.2) {
                double finalY = y;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double x = (radius - 0.14 * finalY) * Math.cos(finalY + 3.14159);
                        double z = (radius - 0.14 * finalY) * Math.sin(finalY + 3.14159);
                        ball.getWorld().spawnParticle(Particle.CLOUD,
                                (float) (loc.getX() + x), (float) (loc.getY() + 0.3),
                                (float) (loc.getZ() + z), 0, 0, 0, 0, 1);
                    }
                }.runTaskLater(VolleyBall.getInstance(), (long) y);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    balls.remove(ball);
                    ball.remove();
                }
            }.runTaskLater(VolleyBall.getInstance(), (long) 6.28);
            
            ball.getWorld().playSound(ball.getLocation(), Sound.BLOCK_SAND_PLACE, 2, 1);
            ball.getWorld().playSound(ball.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2, 1);
        } else {
            balls.remove(ball);
            ball.remove();
        }
    }
    
    /**
     * check if the ball is out
     *
     * @return true if ball is out
     */
    public boolean isOut() {
        return !end;
    }
    
    /**
     * get the set of all alive ball entities
     *
     * @return set of alive ball entities
     */
    public static Set<Entity> getBalls() {
        return balls;
    }
    
}
