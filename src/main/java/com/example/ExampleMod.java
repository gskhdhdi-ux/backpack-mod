package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "modid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Ёмкость ранца и заряд от одной канистры
	public static final int MAX_CHARGE = 1000;
	public static final int CHARGE_PER_CANISTER = 250;

	public static final Item CANISTER = new Item(new Item.Properties().stacksTo(16));
	public static final Item BACKPACK = new BackpackItem(new Item.Properties().durability(MAX_CHARGE));

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.ITEM, id("canister"), CANISTER);
		Registry.register(BuiltInRegistries.ITEM, id("backpack"), BACKPACK);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(BACKPACK);
			entries.accept(CANISTER);
		});
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Ранец: заряд = макс. прочность - полученный урон. ПКМ расходует канистру и заряжает. */
	public static class BackpackItem extends Item {

		public BackpackItem(Item.Properties properties) {
			super(properties);
		}

		public static int getCharge(ItemStack stack) {
			return stack.getMaxDamage() - stack.getDamageValue();
		}

		public static boolean useCharge(ItemStack stack, int amount) {
			if (getCharge(stack) < amount) return false;
			stack.setDamageValue(stack.getDamageValue() + amount);
			return true;
		}

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack pack = player.getItemInHand(hand);

			if (pack.getDamageValue() == 0) {
				return InteractionResultHolder.pass(pack);
			}

			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack slot = player.getInventory().getItem(i);
				if (slot.is(CANISTER)) {
					if (!level.isClientSide) {
						slot.shrink(1);
						pack.setDamageValue(Math.max(0, pack.getDamageValue() - CHARGE_PER_CANISTER));
						level.playSound(null, player.blockPosition(),
								SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
					}
					return InteractionResultHolder.sidedSuccess(pack, level.isClientSide);
				}
			}
			return InteractionResultHolder.fail(pack);
		}
	}
}
