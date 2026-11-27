package net.centerclient.event;

/**
 * Something that happened in the game, like "a tick passed" or "a chat message
 * arrived". Listeners get told about it through the {@link EventBus}.
 *
 * Some events can be cancelled: a listener calls {@link #cancel()} and the
 * game then skips the normal action (for example, a chat message isn't shown).
 *
 * To keep things fast, each kind of event is one object that gets reused
 * every time it happens, so a listener should not keep an event around
 * after it returns.
 */
public abstract class Event {

	private boolean cancelled;

	/** Asks the game to skip the normal action, if this event allows it. */
	public void cancel() {
		this.cancelled = true;
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	/** Called before the event is sent out again. */
	void reset() {
		this.cancelled = false;
	}
}
