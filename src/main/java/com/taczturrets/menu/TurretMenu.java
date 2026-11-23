package com.taczturrets.menu;

import com.taczturrets.TaczTurrets;
import com.taczturrets.blockentity.AbstractTaczTurretBlockEntity;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Turret inventory: one gun slot plus the nine ammo slots inherited from AeroIE's turret block entity.
 * <p>
 * The gun slot is what was missing from AeroIE's own {@code TurretContainerMenu} (it only has ammo
 * slots), which made the installed gun invisible in a GUI and impossible to unload by hand.
 */
public class TurretMenu extends AbstractContainerMenu {
    public static final int GUN_SLOT = 0;
    public static final int AMMO_START = 1;
    public static final int AMMO_COUNT = 9;
    public static final int PLAYER_START = AMMO_START + AMMO_COUNT;

    private static final int GUN_X = 80;
    private static final int GUN_Y = 10;
    private static final int AMMO_Y = 40;
    private static final int PLAYER_Y = 218;
    private static final int HOTBAR_Y = 272;
    private static final int SLOT = 18;

    private final BlockPos pos;
    @Nullable
    private final AbstractTaczTurretBlockEntity turret;

    /** Client side: the block position comes from the menu payload. */
    public TurretMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    /** Server side. */
    public TurretMenu(int containerId, Inventory playerInventory, AbstractTaczTurretBlockEntity turret) {
        this(containerId, playerInventory, turret.getBlockPos());
    }

    private TurretMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(TaczTurrets.TURRET_MENU.get(), containerId);
        this.pos = pos;
        BlockPos origin = pos;
        this.turret = playerInventory.player.level().getBlockEntity(origin) instanceof AbstractTaczTurretBlockEntity found
                ? found : null;
        ItemStackHandler detached = new ItemStackHandler(AMMO_COUNT);

        this.addSlot(new GunSlot(this.turret, GUN_X, GUN_Y));
        for (int column = 0; column < AMMO_COUNT; column++) {
            int x = 8 + column * SLOT;
            this.addSlot(this.turret == null
                    ? new SlotItemHandler(detached, column, x, AMMO_Y)
                    : new SlotItemHandler(this.turret, column, x, AMMO_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * SLOT, PLAYER_Y + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * SLOT, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(this.pos)) <= 64.0D;
    }

    /** The turret backing this menu, or {@code null} when the block entity is not loaded. */
    @Nullable
    public AbstractTaczTurretBlockEntity getTurret() {
        return this.turret;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index == GUN_SLOT || index < PLAYER_START) {
            if (!this.moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof IGun) {
            if (!this.moveItemStackTo(stack, GUN_SLOT, GUN_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, AMMO_START, AMMO_START + AMMO_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    /**
     * Slot for the installed gun. It is not backed by a container: reads and writes go straight to the
     * turret block entity, so the gun shown in the GUI is exactly the gun the turret fires.
     */
    private static final class GunSlot extends Slot {
        @Nullable
        private final AbstractTaczTurretBlockEntity turret;
        private final ItemStackHandler detached;

        GunSlot(@Nullable AbstractTaczTurretBlockEntity turret, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.turret = turret;
            this.detached = new ItemStackHandler(1);
        }

        @Override
        public ItemStack getItem() {
            return this.turret != null ? this.turret.getGunStack() : this.detached.getStackInSlot(0);
        }

        @Override
        public void set(ItemStack stack) {
            if (this.turret != null) {
                this.turret.setGunStack(stack);
            } else {
                this.detached.setStackInSlot(0, stack);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack current = this.getItem();
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack split = current.split(amount);
            this.set(current);
            this.setChanged();
            return split;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof IGun;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            if (this.turret != null) {
                this.turret.setChanged();
            }
        }
    }
}
