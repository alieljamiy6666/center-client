package net.centerclient.event;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Delivers events to the listeners that asked for them.
 *
 * Why it's fast: there's no reflection or annotation scanning (which the
 * browser build can't do well anyway). Each kind of event has a plain array of
 * listeners. Sending an event is just one map lookup and a loop over that
 * array, and nothing new gets created. The arrays are only rebuilt when
 * listeners are added or removed, which is rare (like toggling a module).
 *
 * If a listener crashes, the error is logged and the other listeners still
 * run, so one broken module can't take down the game.
 */
public final class EventBus {

	private static final Logger LOGGER = LogManager.getLogger("Center Client Events");
	private static final Listener<?>[] NONE = new Listener<?>[0];

	private final Map<Class<?>, Listener<?>[]> listeners = new HashMap<Class<?>, Listener<?>[]>();

	/** Starts sending events of this kind to the listener. */
	public <E extends Event> void subscribe(Class<E> type, Listener<E> listener) {
		Listener<?>[] old = listeners.get(type);
		if (old == null) {
			old = NONE;
		}
		for (int i = 0; i < old.length; ++i) {
			if (old[i] == listener) {
				return; // already listening
			}
		}
		Listener<?>[] grown = new Listener<?>[old.length + 1];
		System.arraycopy(old, 0, grown, 0, old.length);
		grown[old.length] = listener;
		listeners.put(type, grown);
	}

	/** Stops sending events of this kind to the listener. */
	public <E extends Event> void unsubscribe(Class<E> type, Listener<E> listener) {
		Listener<?>[] old = listeners.get(type);
		if (old == null) {
			return;
		}
		int index = -1;
		for (int i = 0; i < old.length; ++i) {
			if (old[i] == listener) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			return;
		}
		if (old.length == 1) {
			listeners.remove(type);
			return;
		}
		Listener<?>[] shrunk = new Listener<?>[old.length - 1];
		System.arraycopy(old, 0, shrunk, 0, index);
		System.arraycopy(old, index + 1, shrunk, index, old.length - index - 1);
		listeners.put(type, shrunk);
	}

	/** True if anyone is listening for this kind of event. */
	public boolean hasListeners(Class<? extends Event> type) {
		return listeners.containsKey(type);
	}

	/** Sends an event to everyone listening for its kind, and returns it. */
	@SuppressWarnings("unchecked")
	public <E extends Event> E post(E event) {
		event.reset();
		Listener<?>[] array = listeners.get(event.getClass());
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				try {
					((Listener<E>) array[i]).onEvent(event);
				} catch (Throwable t) {
					LOGGER.error("A listener crashed while handling " + event.getClass().getName());
					LOGGER.error(t);
				}
			}
		}
		return event;
	}
}
