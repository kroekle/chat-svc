package com.kurtspace.cncf.app;

import java.io.IOException;

import com.kurtspace.cncf.svc.ChatService;

import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

public class ChatAppServer {
	
	private final io.grpc.Server server;
	private int port;
	
	public ChatAppServer(int port) {
		this.port = port;
		server = ServerBuilder.forPort(port)
				.addService(new ChatService())
				.addService(ProtoReflectionService.newInstance())
				.build();
	}
	
	public void start() throws IOException {
		server.start();
		System.out.println("Server started on port: "+ port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("*** shutting down gRPC server since JVM is shutting down");
				ChatAppServer.this.stop();
				System.out.println("*** server shut down");
			}
		});
	}

	protected void stop() {
		if (server != null) {
			server.shutdown();
		}
	}
	
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}
	
	public static void main(String[] args) throws InterruptedException, IOException {
		
		ChatAppServer server = new ChatAppServer(9000);
		server.start();
		server.blockUntilShutdown();
	}

}
