package LMS;
/*
 * @author Boqi Wang
 * April. 2014
 * the University of Queensland
 * Code for course: COMS7201
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class LoanServer {

	public LoanServer(String[] args){
		int port=Integer.parseInt(args[0]);
		if(port < 1024 ||port > 65535) {
			System.err.println("Invalid command line argument for Name Server\n");
			System.exit(1);
		}		
		ArrayList<String> Loan=readFile();
		registerServer(port);
		listenforRequest(port,Loan);
	}
	
	/*
	 * waiting for incoming request from a QueryClient
	 */
	private void listenforRequest(int Port,ArrayList<String> Loan) 
	{
		Selector selector =null;
		ServerSocketChannel serverSocketChannel = null;
		ServerSocket serverSocket = null;
		try {
			// open selector
			selector = Selector.open();
			// open socket channel
			serverSocketChannel = ServerSocketChannel.open();
			// set the socket associated with this channel
			serverSocket = serverSocketChannel.socket();
			// set Blocking mode to non-blocking
			serverSocketChannel.configureBlocking(false);
			// bind port
			serverSocket.bind(new InetSocketAddress(9002));
			try
			{
			// registers this channel with the given selector, returning a selection key
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			}catch(Exception e) {
				System.err.print("Cannot listen on given port number"+Port+"\n");
			}
			System.out.print("Loan Server waiting for incoming connections....\n");
			String reply="null";

			while (selector.select() > 0) {
				for (SelectionKey key : selector.selectedKeys()) {
					// test whether this key's channel is ready to accept a new socket connection
					if (key.isAcceptable()) {
						// accept the connection
						ServerSocketChannel server = (ServerSocketChannel) key.channel();
						SocketChannel sc = server.accept();
						if (sc == null)
							continue;
						System.out.println("Connection accepted from: " + sc.getRemoteAddress());
						// set blocking mode of the channel
						sc.configureBlocking(false);
						// allocate buffer
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						// set register status to READ
						sc.register(selector, SelectionKey.OP_READ, buffer);
					}
					// test whether this key's channel is ready for reading from Client
					else if (key.isReadable()) {
						// get allocated buffer with size 1024
						ByteBuffer buffer = (ByteBuffer) key.attachment();
						SocketChannel sc = (SocketChannel) key.channel();
						int readBytes = 0;
						String message = null;
						// try to read bytes from the channel into the buffer
						try {
							int ret;
							try {
								while ((ret = sc.read(buffer)) > 0)
									readBytes += ret;
							} catch (Exception e) {
								readBytes = 0;
							} finally {
								buffer.flip();
							}
							// finished reading, form message
							if (readBytes > 0) {
								message = Charset.forName("UTF-8").decode(buffer).toString();
								buffer = null;
							}
						} finally {
							if (buffer != null)
								buffer.clear();
						}
						// react by Client's message
						if (readBytes > 0) {
								reply=lookup(message.split(",")[1],message.split(",")[2], Loan);
							buffer = null;
							sc.register(selector, SelectionKey.OP_WRITE, reply);
							}
					}
			
					// test whether this key's channel is ready for sending to Client
					else if (key.isWritable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						buffer.put(((String) key.attachment()).getBytes());
						buffer.flip();
						sc.write(buffer);
						// close connection and waiting for incoming connections
						sc.close();
						System.err.print("LoanServer connection closed\n");
						System.err.print("LoanServer waiting for incoming connections ...\n");
					}
				}
				if (selector.isOpen()) {
					selector.selectedKeys().clear();
				} else {
					break;
				}
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (serverSocketChannel != null) {
				try {
					serverSocketChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	
	/*
	 * reading the loan file into program
	 */
	@SuppressWarnings("resource")
	private static ArrayList<String> readFile()
	{
		ArrayList<String> Loan = new ArrayList<String>();
		
		BufferedReader reader=null;
		try 
		{
			reader=new BufferedReader(new FileReader("loans-file.txt"));
			String item;
			while((item=reader.readLine())!=null)
			{
				 
				Loan.add(item);
			}
		} catch(FileNotFoundException e){
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return Loan;
	}
	
	/*
	 * the method to lookup the Loan file to find reuslt
	 */
	private static String lookup(String type,String keyword,ArrayList<String> data)
	{
		StringBuilder Result=new StringBuilder();
		if(type.equals("L"))
		{
			for(String piece :data)
			{
				if(piece.split(" ")[0].contains(keyword)) 
				Result.append(piece+"\n");
			}
		}
		else 
		{
			for(String piece :data)
			{
				if(piece.split(" ")[1].contains(keyword)) 
				Result.append(piece+"\n");
			}
		}
		if(Result.length()==0)
		{
			Result.append("Sorry, no record are found");
		}
		return Result.toString();
	}

	/*
	 * register the loan server in nameServer
	 */
	private void registerServer(int Port)
	{
		SocketChannel channel = null;
		String Reply=null;
		String query="R,LoanServer,"+9002;
		try {
			// set port from connection
			int port =Port;
			// open socket channel
			channel = SocketChannel.open();
			// set Blocking mode to non-blocking
			channel.configureBlocking(false);
			// set Server info
			InetSocketAddress target = new InetSocketAddress("localHost", port);
			// open selector
			Selector selector = Selector.open();
			// connect to Server
			channel.connect(target);
			try
			{
			// registers this channel with the given selector, returning a selection key
			channel.register(selector, SelectionKey.OP_CONNECT);
			} catch(Exception e) {
				System.err.print("Cannot connect to name server located at"+port+"\n");
			}

			while (selector.select() > 0) {
				for (SelectionKey key : selector.selectedKeys()) {
					// test connectivity
					if (key.isConnectable()) {
						SocketChannel sc = (SocketChannel) key.channel();
//						sc.configureBlocking(true);
						// set register status to WRITE
						sc.register(selector, SelectionKey.OP_WRITE);
						sc.finishConnect();
					}
					// test whether this key's channel is ready for reading from Server
					else if (key.isReadable()) {
						// allocate a byte buffer with size 1024
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						SocketChannel sc = (SocketChannel) key.channel();
						int readBytes = 0;
						// try to read bytes from the channel into the buffer
						try {
							int ret = 0;
							try {
								while ((ret = sc.read(buffer)) > 0)
									readBytes += ret;
							} finally {
								buffer.flip();
							}
							// finished reading, print to Client
							if (readBytes > 0) {
								Reply=Charset.forName("UTF-8").decode(buffer).toString();
								System.out.println(Reply);
								buffer = null;
								if(Reply.equalsIgnoreCase("Done")) {
									selector.close();
								}	
							}
						} finally {
							if (buffer != null)
								buffer.clear();
						}
					}
					// test whether this key's channel is ready for writing to Server
					else if (key.isWritable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						// send to Server
						channel.write(Charset.forName("UTF-8").encode(query));
						// set register status to READ
						sc.register(selector, SelectionKey.OP_READ);					}
				}
				if (selector.isOpen()) {
					selector.selectedKeys().clear();
				} else {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		new LoanServer(args);
	}

}
