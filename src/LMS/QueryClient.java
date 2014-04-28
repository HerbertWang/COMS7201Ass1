package LMS;
/*
 * @author Boqi Wang
 * April. 2014
 * the University of Queensland
 * Code for course: COMS7201
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class QueryClient {

	public QueryClient(String[] args) {
		if(args.length<3) {
			System.out.println("Invalid command line argument");
			System.exit(1);
		}
		
		int port=Integer.parseInt(args[0]);
		String request=null;
		String keyword=null;
		
			String valid="LDCK";
			
			if(args[1].length()>1||!valid.contains(args[1])) {
				System.out.println("Invalid command line argument");
				System.exit(1);
			} else {
				request=args[1];
				keyword=args[2];
			}
		System.out.println(port+","+request+","+keyword);
		
		
		//process the request
		String query="Lookup,"+request+","+keyword;
		
		String response=setUpConnection(query,port);
		
		if(response.contains("Error"))
		{
			if(request.split(",")[0].equals("L")||request.split(",")[0].equals("D")) {
				System.err.println("QueryClient unable to connect to LoanServer");
				System.exit(1);
			}
			else if(request.split(",")[0].equals("C")||request.split(",")[0].equals("K")) {
				System.err.println("QueryClient unable to connect to CatalogServer");
				System.exit(1);
			}
		}
		
		String result=setUpConnection(query,Integer.parseInt(response.split(",")[2]));
		
		if(!result.contains("Sorry, no record are found"))
		{
		//request appropriate book title and author information for any titles returned
		if(request.split(",")[0].equals("L")||request.split(",")[0].equals("D"))
		{
			query="Lookup,"+"C"+","+result.split(" ")[1];
			String UID=result.split(" ")[0];
			String date=result.split(" ")[2];	
			response=setUpConnection(query,port);
			if(response.contains("Error"))
			{
				System.err.println("QueryClient unable to connect to CatalogServer");
				System.exit(1);
			}
			//request the book information based on book ID
			result=setUpConnection(query,Integer.parseInt(response.split(",")[2]));
			
			System.out.println("User ID:"+UID+"\n"
					+"You loan book list"+result
					+"due to "+date);
		}
		else
		{
			System.out.println(result);
		}
		}
		else
		{
			System.out.println(result);
		}
		
	}
	
	/*
	 * the method setup a connection to Server
	 */
	public String setUpConnection(String query,int Port)
	{
		SocketChannel channel = null;
		String Reply=null;
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
			// registers this channel with the given selector, returning a selection key
			channel.register(selector, SelectionKey.OP_CONNECT);

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
								buffer = null;
								selector.close();
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
						sc.register(selector, SelectionKey.OP_READ);
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
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return Reply;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new QueryClient(args);
	}

}