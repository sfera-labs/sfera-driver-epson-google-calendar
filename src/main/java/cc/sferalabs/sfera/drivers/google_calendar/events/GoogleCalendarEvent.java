/*-
 * +======================================================================+
 * Google Calendar
 * ---
 * Copyright (C) 2016 Sfera Labs S.r.l.
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * -======================================================================-
 */

package cc.sferalabs.sfera.drivers.google_calendar.events;

import com.google.api.services.calendar.model.Events;

import cc.sferalabs.sfera.events.BaseEvent;
import cc.sferalabs.sfera.events.Node;

/**
 * Event triggered when the Telegram Bot receives a message.
 * 
 * @sfera.event_id cal_name name of the calendar linked to the event (white
 *                 spaces replaced by '-')
 * @sfera.event_val events_obj see getValue()
 * @sfera.event_val_simple events_titles see getSimpleValue()
 * 
 * @author Giampiero Baggiani
 *
 * @version 1.0.0
 *
 */
public class GoogleCalendarEvent extends BaseEvent {

	private final Events events;

	/**
	 * 
	 * @param source
	 *            the source node
	 * @param id
	 *            the calendar name
	 * @param events
	 *            the ongoing events
	 */
	public GoogleCalendarEvent(Node source, String id, Events events) {
		super(source, id);
		this.events = events;
	}

	/**
	 * Returns the {@link Events} object containing all the ongoing calendar
	 * events at the time this event was triggered.
	 * 
	 * @return the {@link Events} object containing all the ongoing calendar
	 *         events at the time this event was triggered
	 * 
	 * @see <a href=
	 *      "https://developers.google.com/resources/api-libraries/documentation/calendar/v3/java/latest/com/google/api/services/calendar/model/Events.html">
	 *      Events class JavaDoc</a>
	 * 
	 */
	@Override
	public Events getValue() {
		return events;
	}

	/**
	 * Returns a String representation of the list of the titles of the calendar
	 * events contained in this event's value.
	 * <p>
	 * For instance: "[ event1, event2 ]"
	 * 
	 * @return a String representation of the list of the titles of the calendar
	 *         events contained in this event's value
	 */
	@Override
	public String getSimpleValue() {
		return events.getItems().toString();
	}
}
