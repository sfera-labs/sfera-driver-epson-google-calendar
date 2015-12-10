package cc.sferalabs.sfera.drivers.google.calendar.events;

import java.util.List;

import cc.sferalabs.sfera.events.BaseEvent;
import cc.sferalabs.sfera.events.Node;

public class GoogleCalendarEvent extends BaseEvent {

	private final List<String> events;

	/**
	 * 
	 * @param source
	 * @param id
	 * @param events
	 */
	public GoogleCalendarEvent(Node source, String id, List<String> events) {
		super(source, id);
		this.events = events;
	}

	@Override
	public List<String> getValue() {
		return events;
	}

}
