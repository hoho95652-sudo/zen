package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Pick any item in the game, set 1-60 seconds, start. */
public class AuctionScreen extends Screen {
	private static final int ROWS = 8;
	private final List<Item> all = new ArrayList<>();
	private List<Item> filtered = new ArrayList<>();
	private final List<Button> rowButtons = new ArrayList<>();
	private EditBox search;
	private Button selectedLabel, timeLabel;
	private int page = 0;
	private int seconds = 30;
	private Item selected = null;

	public AuctionScreen() {
		super(Component.literal("Start Auction"));
		for (Item i : BuiltInRegistries.ITEM) if (i != Items.AIR) all.add(i);
		filtered = new ArrayList<>(all);
	}

	private static String nameOf(Item i) {
		return i.getDefaultInstance().getHoverName().getString();
	}

	@Override
	protected void init() {
		int left = width / 2 - 210, right = width / 2 + 10;

		search = new EditBox(font, left, 20, 200, 18, Component.literal("Search"));
		search.setHint(Component.literal("Search items..."));
		search.setResponder(text -> {
			String q = text.toLowerCase().trim();
			filtered = new ArrayList<>();
			for (Item i : all) if (nameOf(i).toLowerCase().contains(q)) filtered.add(i);
			page = 0;
			refreshRows();
		});
		addRenderableWidget(search);

		addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> { if (page > 0) { page--; refreshRows(); } })
				.bounds(left, 48 + ROWS * 22, 98, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
			if ((page + 1) * ROWS < filtered.size()) { page++; refreshRows(); }
		}).bounds(left + 102, 48 + ROWS * 22, 98, 20).build());

		selectedLabel = addRenderableWidget(Button.builder(Component.literal("Item: (none)"), b -> {})
				.bounds(right, 48, 200, 20).build());
		selectedLabel.active = false;

		timeLabel = addRenderableWidget(Button.builder(Component.literal(""), b -> {})
				.bounds(right, 80, 200, 20).build());
		timeLabel.active = false;
		addRenderableWidget(Button.builder(Component.literal("-5"), b -> setSeconds(seconds - 5)).bounds(right, 104, 48, 20).build());
		addRenderableWidget(Button.builder(Component.literal("-1"), b -> setSeconds(seconds - 1)).bounds(right + 50, 104, 48, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+1"), b -> setSeconds(seconds + 1)).bounds(right + 102, 104, 48, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+5"), b -> setSeconds(seconds + 5)).bounds(right + 152, 104, 48, 20).build());
		setSeconds(seconds);

		addRenderableWidget(Button.builder(Component.literal("Start Auction"), b -> {
			if (selected == null) return;
			AuctionManager.start(Minecraft.getInstance(), nameOf(selected), seconds);
			onClose();
		}).bounds(right, 140, 200, 24).build());

		addRenderableWidget(Button.builder(Component.literal("Cancel running auction"), b -> {
			AuctionManager.cancel(Minecraft.getInstance());
			onClose();
		}).bounds(right, 170, 200, 20).build());

		refreshRows();
	}

	private void setSeconds(int s) {
		seconds = Math.max(1, Math.min(60, s));
		if (timeLabel != null) timeLabel.setMessage(Component.literal("Duration: " + seconds + "s (1-60)"));
	}

	private void refreshRows() {
		for (Button b : rowButtons) removeWidget(b);
		rowButtons.clear();
		int left = width / 2 - 210;
		int start = page * ROWS;
		for (int r = 0; r < ROWS && start + r < filtered.size(); r++) {
			Item item = filtered.get(start + r);
			Button b = Button.builder(Component.literal(nameOf(item)), btn -> {
				selected = item;
				selectedLabel.setMessage(Component.literal("Item: " + nameOf(item)));
			}).bounds(left, 48 + r * 22, 200, 20).build();
			rowButtons.add(addRenderableWidget(b));
		}
	}
}
