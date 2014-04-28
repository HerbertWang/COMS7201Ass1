package LMS;
/*
 * @author Boqi Wang
 * April. 2014
 * the University of Queensland
 * Code for course: COMS7201
 */


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class NameServer {
	
	// set Server parameters
	private static int port = 0;
	private Selector selector = null;
	private ServerSocketChannel serverSocketChannel = null;
	private ServerSocket serverSocket = null;
	private ArrayList<String> LoanServersList=null;
	private ArrayList<String> CatalogServersList=null;


	public NameServer() {
		LoanServersList=new ArrayList<String>();
		CatalogServersList=new ArrayList<String>();
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
			serverSocket.bind(new InetSocketAddress(port));
			try {
				// registers this channel with the given selector, returning a selection key			
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			} catch(Exception e) 
			{
				System.err.print("Cannot listen on given port number"+port+"\n");
			}
			System.out.print("Name Server waiting for incoming connections....\n");
			
			String reply=null;

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
							System.out.println("Message from Client" + sc.getRemoteAddress() + ": " + message);
							
							//deal with register query
							if(message.split(",")[0].equals("R"))
							{
								if(message.split(",")[1].equals("LoanServer"))
								{
									InetAddress proxy = sc.socket().getInetAddress();
									LoanServersList.add(proxy.getHostName()+","+proxy.getHostAddress()+","+message.split(",")[2]);
								}
								if(message.split(",")[1].equals("CatalogServer"))
								{
									InetAddress proxy = sc.socket().getInetAddress();
									CatalogServersList.add(proxy.getHostName()+","+proxy.getHostAddress()+","+message.split(",")[2]);
								}
								reply="Done";
							
							}
							
							//deal with lookup query
							else if(message.split(",")[0].equals("Lookup"))
							{
								if(message.split(",")[1].equals("L")||message.split(",")[1].equals("D")) 
									if(LoanServersList.size()>0) {
										//obtain a LoanServer and re-enqueue it into list
										reply=LoanServersList.remove(0);
										LoanServersList.add(reply);
									}
									else {
										reply="Error: Process has not registered with the Name Server\n";
								}	
							if(message.split(",")[1].equals("C")||message.split(",")[1].equals("K")) 
								if(CatalogServersList.size()>0) {
									//obtain a LoanServer and re-enqueue it into list
									reply=CatalogServersList.remove(0);
									CatalogServersList.add(reply);
								}
								else {
									reply="Error: Process has not registered with the Name Server\n";
								}	
							}
							//deal with the invalid format message
							else
							{
								reply ="Error: please send a message with valid format";
							}
								// set register status to WRITE
								sc.register(key.selector(), SelectionKey.OP_WRITE,reply);
				
						}
					}
					// test whether this key's channel is ready for sending to Client
					else if (key.isWritable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						buffer.put(((String) key.attachment()).getBytes());
						buffer.flip();
						sc.write(buffer);
						// set register status to READ
						sc.register(key.selector(), SelectionKey.OP_READ, buffer);
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		port=Integer.parseInt(args[0]);
		if(port < 1024 ||port > 65535) {
			System.err.println("Invalid command line argument for Name Server\n");
			System.exit(1);
		}
		
		new NameServer();
	}
}