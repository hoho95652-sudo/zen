package com.donutauction;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionManager {
	/**
	 * Matches the server's payment message. CHANGE THIS to match what DonutSMP actually prints
	 * when someone pays you. Group 1 = payer name, group 2 = amount (supports 1,500 / 2.5K / 3M / 1B).
	 */
	static final Pattern PAYMENT = Pattern.compile("(\\w{3,16}) (?:paid|sent) you \\$?([\\d,.]+\\s?[KkMmBb]?)");

	static boolean active;
	static String itemName;
	static String seller;
	static long endMillis;
	static String topBidder;
	static double topBid;
	static boolean warned10, warned5;

	public static void start(Minecraft mc, String item, int seconds) {
		if (active) { local(mc, "An auction is already running."); return; }
		active = true;
		itemName = item;
		seller = mc.getUser().getName();
		endMillis = System.currentTimeMillis() + seconds * 1000L;
		topBidder = null;
		topBid = 0;
		warned10 = seconds <= 10;
		warned5 = seconds <= 5;
		say(mc, "[AUCTION] " + item + " - " + seconds + "s - bid with /pay " + seller + " <amount>");
	}

	public static void cancel(Minecraft mc) {
		if (!active) return;
		active = false;
		say(mc, "[AUCTION] Cancelled.");
	}

	public static void onChat(Minecraft mc, String line) {
		if (!active) return;
		Matcher m = PAYMENT.matcher(line);
		if (!m.find()) return;
		String payer = m.group(1);
		if (payer.equalsIgnoreCase(seller)) return;
		double amount = parseAmount(m.group(2));
		if (amount > topBid) {
			topBid = amount;
			topBidder = payer;
			say(mc, "[AUCTION] New high bid: " + payer + " - " + format(amount) + " for " + itemName);
		}
	}

	public static void tick(Minecraft mc) {
		if (!active || mc.player == null) return;
		long left = endMillis - System.currentTimeMillis();
		if (!warned10 && left <= 10_000) { warned10 = true; say(mc, "[AUCTION] 10s left - " + status()); }
		if (!warned5 && left <= 5_000) { warned5 = true; say(mc, "[AUCTION] 5s left - " + status()); }
		if (left <= 0) {
			active = false;
			if (topBidder == null) say(mc, "[AUCTION] Ended with no bids on " + itemName + ".");
			else say(mc, "[AUCTION] SOLD! " + itemName + " to " + topBidder + " for " + format(topBid) + "!");
			if (topBidder != null) local(mc, "Give " + itemName + " to " + topBidder + ". Their payment is already in.");
		}
	}

	private static String status() {
		return topBidder == null ? "no bids yet" : "top bid " + format(topBid) + " by " + topBidder;
	}

	static double parseAmount(String raw) {
		String s = raw.replace(",", "").replace(" ", "").toUpperCase();
		double mult = 1;
		if (s.endsWith("K")) mult = 1e3;
		else if (s.endsWith("M")) mult = 1e6;
		else if (s.endsWith("B")) mult = 1e9;
		if (mult != 1) s = s.substring(0, s.length() - 1);
		try { return Double.parseDouble(s) * mult; } catch (NumberFormatException e) { return 0; }
	}

	static String format(double v) {
		if (v >= 1e9) return trim(v / 1e9) + "B";
		if (v >= 1e6) return trim(v / 1e6) + "M";
		if (v >= 1e3) return trim(v / 1e3) + "K";
		return trim(v);
	}

	private static String trim(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
	}

	private static void say(Minecraft mc, String msg) {
		if (mc.player != null) mc.player.connection.sendChat(msg);
	}

	private static void local(Minecraft mc, String msg) {
		if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), false);
	}
}
