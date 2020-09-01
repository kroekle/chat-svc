package com.kurtspace.cncf.svc;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Empty;
import com.kurtspace.cncf.chat.Chat.ChatResults;
import com.kurtspace.cncf.chat.Chat.Message;
import com.kurtspace.cncf.chat.Chat.People;
import com.kurtspace.cncf.chat.Chat.Person;
import com.kurtspace.cncf.chat.Chat.Register;
import com.kurtspace.cncf.chat.ChatServiceGrpc.ChatServiceImplBase;
import com.kurtspace.cncf.store.ChatStore;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class ChatService extends ChatServiceImplBase {

	private ChatStore store = new ChatStore();
	
	@Override
	public void ping(Empty request, StreamObserver<Empty> responseObserver) {
		responseObserver.onNext(Empty.newBuilder().build());
		responseObserver.onCompleted();
	}
	
	@Override
	public void getActivePeople(Empty request, StreamObserver<People> responseObserver) {
		responseObserver.onNext(getPeopleSorted(store.getActivePeople()));
		responseObserver.onCompleted();
	}

	private People getPeopleSorted(Set<Person> people) {
		return People.newBuilder()
				.addAllPerson(people.stream()
				.sorted(new ChatStore.ActiveNameComparator())
				.collect(Collectors.toList())
				).build();
	}
	
	@Override
	public void changeStatus(Person request, StreamObserver<Empty> responseObserver) {
		store.registerPerson(request);
		responseObserver.onNext(Empty.newBuilder().build());
		responseObserver.onCompleted();
	}

	@Override
	public void sendMessage(Message request, StreamObserver<Empty> responseObserver) {
		store.sendMessage(request.getSender(), request.getText());
		responseObserver.onNext(Empty.newBuilder().build());
		responseObserver.onCompleted();
	}
	
	@Override
	public void registerAndStream(Register request, StreamObserver<ChatResults> responseObserver) {
		
		if (!request.hasPerson()) {
			responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT));
		} else {
			store.registerMessageListener(new ChatStore.MessageListener() {
				
				@Override
				public void onPeopleChange(Set<Person> people) {
					responseObserver.onNext(ChatResults.newBuilder().
							setPeople(
									getPeopleSorted(people))
							.build());
				}

				@Override
				public void onMessage(Person person, String message) {
					
					responseObserver.onNext(
						ChatResults.newBuilder()
							.setText(Message.newBuilder()
									.setSender(person)
									.setText(message))
							.build());
				}
			});
			store.registerPerson(request.getPerson());
		}
	}
}