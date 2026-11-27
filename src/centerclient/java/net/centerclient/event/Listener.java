package net.centerclient.event;

/**
 * Something that wants to hear about one kind of event.
 * Usually written as a method reference, like {@code this::onTick}.
 */
public interface Listener<E extends Event> {

	void onEvent(E event);
}
