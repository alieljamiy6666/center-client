package net.centerclient.event;

import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;

/**
 * Sent when a chat message arrives from the server, before it's shown.
 * Listeners can change the message (for example, add a timestamp) or cancel
 * it so it isn't shown at all.
 */
public final class ChatReceiveEvent extends Event {

	private ChatType type;
	private ITextComponent message;

	ChatReceiveEvent set(ChatType type, ITextComponent message) {
		this.type = type;
		this.message = message;
		return this;
	}

	/** Where the message goes: normal chat, system message, or the action bar above the hotbar. */
	public ChatType getType() {
		return type;
	}

	public ITextComponent getMessage() {
		return message;
	}

	/** Replaces the message that will be shown. */
	public void setMessage(ITextComponent message) {
		this.message = message;
	}
}
