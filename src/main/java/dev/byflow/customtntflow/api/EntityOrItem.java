package dev.byflow.customtntflow.api;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Simple wrapper that can represent either an entity or an item when querying the API.
 */
public final class EntityOrItem {

    private final Entity entity;
    private final ItemStack itemStack;

    private EntityOrItem(Entity entity, ItemStack itemStack) {
        this.entity = entity;
        this.itemStack = itemStack == null ? null : itemStack.clone();
    }

    public static EntityOrItem of(Entity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }
        return new EntityOrItem(entity, null);
    }

    public static EntityOrItem of(ItemStack itemStack) {
        if (itemStack == null) {
            throw new IllegalArgumentException("itemStack");
        }
        return new EntityOrItem(null, itemStack);
    }

    public Optional<Entity> entity() {
        return Optional.ofNullable(entity);
    }

    public Optional<ItemStack> itemStack() {
        return Optional.ofNullable(itemStack == null ? null : itemStack.clone());
    }
}
