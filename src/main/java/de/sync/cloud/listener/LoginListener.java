package de.sync.cloud.listener;

import de.sync.cloud.permissionsystem.PermissionManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Set;
import java.util.UUID;

public class LoginListener implements Listener {

    private final PermissionManager permissionManager;

    public LoginListener(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @EventHandler
    public void onLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Permissions neu laden und setzen
        permissionManager.reload(); // optional, falls du ganz sicher gehen willst
        Set<String> perms = permissionManager.getPermissions(uuid);

        for (String perm : perms) {
            player.setPermission(perm, true); // Methode vom CustomPermissionManager
        }
    }
}
