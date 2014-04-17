package LMS;
/**
 * 
 * @author Boqi Wang
 * April.2014
 * the University of Queensland
 * COMS7201 Computer Network I
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
import java.util.List;


public class NameServer {

	// set Server parameters
	private int port;
	private Selector selector = null;
	private ServerSocketChannel serverSocketChannel = null;
	private ServerSocket serverSocket = null;
	
	/**
	 * @param args
	 */
	public NameServer(int port)
	{
		List<String> runningLoanServers = new ArrayList<String>();
		List<String> runningCatalogServers = new ArrayList<String>();
		
		this.port=port;
		if(port<=1023||port>=65535)
		{
			System.err.println("Invalid command line argument" +
					" for Name Server\n");
			System.exit(1);
		}
		
		try
		{
			//open selector
			selector=Selector.open();
			//open socket channel
			serverSocketChannel=ServerSocketChannel.open();
			//set the socket associated with this channel
			serverSocket=serverSocketChannel.socket();
			//set Blocking mode to non-blocking
			serverSocketChannel.configureBlocking(false);
			//bind port
			serverSocket.bind(new InetSocketAddress(port));
			try{
				// registers this channel with the given selector, returning a selection key
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
				System.out.println("Name Server waiting for" +
						" incoming connections ...\n");
			}catch(Exception e){
				System.err.println("Cannot listen on given" +
						"port number"+this.port+"\n");
				System.exit(1);
			}
			
			while(selector.select()>0) {
				for(SelectionKey key : selector.selectedKeys()) {
					// test whether this key's channel is ready to accept a new socket connection
					if(key.isAcceptable())
					{
						// accept the connection
						ServerSocketChannel server = 
								(ServerSocketChannel) key.channel();
						SocketChannel sc=server.accept();
						if(sc==null) continue;
						sc.configureBlocking(false);
						//allocate buffer
						ByteBuffer buffer=ByteBuffer.allocate(1024);
						//set register status to READ
						sc.register(selector, SelectionKey.OP_READ,buffer);
					}
					else if(key.isReadable()) {
						//get allocated buffer with size 1024
						ByteBuffer buffer=(ByteBuffer)key.attachment();
						SocketChannel sc=(SocketChannel)key.channel();
						
						int readBytes=0;
						String message=null;
						// try to read bytes from the channel into the buffer
						try {
							int ret;
							try{
								while((ret=sc.read(buffer))>0)
									readBytes+=ret;
							}catch (Exception e)
							{
								readBytes=0;
							}finally {
								buffer.flip();
							}
							String reply=null;
							//finished reading from message
							if (readBytes > 0) {
								message = Charset.forName("UTF-8").decode(buffer).toString();
								System.out.println(message.split(",")[0]);
								if(message.split(",")[0].equals("register")) {
									if(message.split(",")[1].equals("loanserver")) {

										InetAddress remote = sc.socket().getInetAddress();
										runningLoanServers.add(remote.getHostName()+","+remote.getHostAddress()+","+message.split(",")[2]);

									}
									if(message.split(",")[1].equals("catalogserver")) {

										InetAddress remote = sc.socket().getInetAddress();
										runningCatalogServers.add(remote.getHostName()+","+remote.getHostAddress()+","+message.split(",")[2]);

									}
									
									reply = "success";

								}

								if(message.split(",")[0].equals("lookup")) {

									if(message.split(",")[1].equals("loanserver")) {

										if(runningLoanServers.size() > 0) {
											//taking the first loan server and sending it to the back of the process
											//so that if there are more then one servers registered..the load is 
											//distributed 
											reply = runningLoanServers.remove(0);
											runningLoanServers.add(reply);
											
										} else {
											
											reply = "Error: Process has not registered with the Name Server\n";
											
											
										}


									}
									
									if(message.split(",")[1].equals("catalogserver")) {

										if(runningCatalogServers.size() > 0) {
											//taking the first loan server and sending it to the back of the process
											//so that if there are more then one servers registered..the load is 
											//distributed 
											reply = runningCatalogServers.remove(0);
											System.out.println(reply);
											runningCatalogServers.add(reply);
									
											
										} else {
											
											reply = "Error: Process has not registered with the Name Server\n";
											
											
										}


									}


								}
								//socketChannel.close();
								buffer = null;
								System.out.println(reply);
								sc.register(selector, SelectionKey.OP_WRITE, reply);
							}

							System.out.println(message);

						}finally {
							if(buffer!=null)
								buffer.clear();
						}
						
						
					}
					else if(key.isWritable()) {
						SocketChannel sc = (SocketChannel) key.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						buffer.put(((String) key.attachment()).getBytes());
						buffer.flip();
						sc.write(buffer);
						// set register status to READ
						
						sc.close();
						
						System.out.println(runningLoanServers);
						
					}
				}
			
			
			}
			
			
		}catch(IOException e){
			e.printStackTrace();
		} 
	}
	
	public static void main(String[] args) {
		int arg1=Integer.parseInt(args[0]);
		new NameServer(arg1);
	}
}
