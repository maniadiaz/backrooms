package io.github.maniadiaz.bedroom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

final class SurvivalItems {
    enum Kind { ALMOND_WATER, ENERGY_DRINK, BANDAGE, BATTERY, FLASHLIGHT }
    static final int FULL_CHARGE = 24000;
    static int charge(ItemStack stack) { return stack.hasTag() && stack.getTag().contains("Charge") ? Math.max(0, Math.min(FULL_CHARGE, stack.getTag().getInt("Charge"))) : FULL_CHARGE; }
    static boolean on(ItemStack stack) { return stack.hasTag() && stack.getTag().getBoolean("On"); }

    static final class Supply extends Item {
        final Kind kind;
        Supply(Kind kind) { super(new Item.Properties().stacksTo(kind == Kind.FLASHLIGHT ? 1 : 16)); this.kind = kind; }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer p && !SurvivalSystem.active(p)) return InteractionResultHolder.fail(stack);
            if (kind == Kind.FLASHLIGHT) {
                if (!level.isClientSide) stack.getOrCreateTag().putBoolean("On", !on(stack) && charge(stack) > 0);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (kind == Kind.BATTERY) {
                if (!level.isClientSide && recharge(player)) { if (!player.getAbilities().instabuild) stack.shrink(1); }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            if (kind == Kind.BANDAGE && player.getHealth() >= player.getMaxHealth()) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand); return InteractionResultHolder.consume(stack);
        }
        @Override public int getUseDuration(ItemStack stack) { return kind == Kind.BANDAGE ? 40 : 32; }
        @Override public UseAnim getUseAnimation(ItemStack stack) { return kind == Kind.BANDAGE ? UseAnim.BOW : UseAnim.DRINK; }
        @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (entity instanceof ServerPlayer p && SurvivalSystem.active(p)) {
                SurvivalState s = MatchData.get(p.server).members.get(p.getUUID()).survival;
                switch (kind) {
                    case ALMOND_WATER -> {
                        s.recover(30); p.getFoodData().eat(6, .2F);
                        SurvivalSystem.removeOwnEffects(p, s);
                        p.removeEffect(MobEffects.HUNGER); p.removeEffect(MobEffects.CONFUSION); p.removeEffect(MobEffects.WEAKNESS);
                    }
                    case ENERGY_DRINK -> {
                        s.energyTicks = 600;
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
                        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 600, 0));
                    }
                    case BANDAGE -> p.heal(6);
                    default -> { return stack; }
                }
                s.quietTicks = 0;
                if (!p.getAbilities().instabuild) stack.shrink(1);
                MatchData.get(p.server).setDirty(); BedroomNetwork.sync(p);
            }
            return stack;
        }
    }

    static boolean recharge(Player p) {
        ItemStack lamp = p.getMainHandItem().is(Bedroom.FLASHLIGHT.get()) ? p.getMainHandItem()
                : p.getOffhandItem().is(Bedroom.FLASHLIGHT.get()) ? p.getOffhandItem() : ItemStack.EMPTY;
        if (lamp.isEmpty() || charge(lamp) == FULL_CHARGE) {
            lamp = ItemStack.EMPTY;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack candidate = p.getInventory().getItem(i);
                if (candidate.is(Bedroom.FLASHLIGHT.get()) && charge(candidate) < FULL_CHARGE) { lamp = candidate; break; }
            }
        }
        if (lamp.isEmpty() || charge(lamp) == FULL_CHARGE) return false;
        lamp.getOrCreateTag().putInt("Charge", Math.min(FULL_CHARGE, charge(lamp) + FULL_CHARGE / 4));
        p.getInventory().setChanged(); return true;
    }
    private SurvivalItems() {}
}
