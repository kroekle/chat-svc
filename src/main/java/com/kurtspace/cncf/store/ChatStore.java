package com.kurtspace.cncf.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.kurtspace.cncf.chat.Chat.Active;
import com.kurtspace.cncf.chat.Chat.Person;

import io.grpc.StatusRuntimeException;

public class ChatStore {
	
	List<MessageListener> messageListeners = Collections.synchronizedList(new ArrayList<>());
	
	Set<StorePerson> people = Collections.synchronizedSet(new HashSet<>());

	public void registerPerson(Person person) {
		StorePerson newPerson = new StorePerson(person);
		synchronized (people) {
			people.remove(newPerson);
			people.add(newPerson);
		}
		
		synchronized (messageListeners) {
			
			for (Iterator<MessageListener> it = messageListeners.listIterator(); it.hasNext();) {
				try {
					
					MessageListener next = it.next();
					// I think this null is happening when multiple streams are running
					if (next != null) {
						next.onPeopleChange(getActivePeople());
					}
				} catch (StatusRuntimeException e) {
					it.remove();
				}
			}
		}
	}

	public Set<Person> getActivePeople() {
		synchronized (people) {
			return people.stream().map(p -> p.toPerson()).collect(Collectors.toSet());
		}
	}

	public void registerMessageListener(MessageListener messageListener) {
		messageListeners.add(messageListener);
	}

	public void sendMessage(Person sender, String text) {
		
		synchronized (messageListeners) {
			for (Iterator<MessageListener> it = messageListeners.listIterator(); it.hasNext();) {
				try {
					MessageListener next = it.next();
					if (next != null) {
						next.onMessage(people.stream()
												.filter(p -> p.name.equals(sender.getName()))
												.map(p -> p.toPerson())
												.findFirst()
												.orElse(sender), text);
					}
				} catch (StatusRuntimeException e) {
					it.remove();
				}
			}
		}
	}

	public static interface MessageListener {
		
		void onMessage(Person person, String message);

		void onPeopleChange(Set<Person> people);
	}
	
	public static final class ActiveNameComparator implements Comparator<Person> {

		@Override
		public int compare(Person p1, Person p2) {
			return p1.getActive().compareTo(p2.getActive())!=0?
					p1.getActive().compareTo(p2.getActive())
					:
					p1.getName().compareTo(p2.getName())
					;
		}
	}
	
	private static final class StorePerson {
		private String name;
		private Active active;
		
		private StorePerson(Person person) {
			name = person.getName();
			active = person.getActive();
		}
		
		private Person toPerson() {
			return Person.newBuilder().setName(name).setActive(active).build();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StorePerson) {
				
				return name.equals(((StorePerson)obj).name);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}
}
