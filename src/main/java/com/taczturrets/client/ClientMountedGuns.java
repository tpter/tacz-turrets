package com.taczturrets.client;

import com.taczturrets.mount.MountedGunSyncPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Client mirror of the guns mounted on CBC cannon mounts, fed by {@link MountedGunSyncPayload}. */
public final class ClientMountedGuns {
    public record Entry(ItemStack gun, float yaw, float pitch, float scale, double worldX, double worldY, double worldZ) {
    }

    private static final Map<BlockPos, Entry> GUNS = new HashMap<>();

    private ClientMountedGuns() {
    }

    public static void update(MountedGunSyncPayload payload) {
        if (payload.remove() || payload.gun().isEmpty()) {
            GUNS.remove(payload.pos());
        } else {
            GUNS.put(payload.pos(),
                    new Entry(payload.gun(), payload.yaw(), payload.pitch(), payload.scale(),
                            payload.worldX(), payload.worldY(), payload.worldZ()));
        }
    }

    public static void clear() {
        GUNS.clear();
    }

    public static Map<BlockPos, Entry> view() {
        return GUNS;
    }
}
