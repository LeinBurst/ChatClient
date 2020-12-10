import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  // Create a new Selector for selecting
  static private Selector selector;

  static private HashMap<SocketChannel, User> users = new HashMap<>();

  static public void main( String args[] ) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt( args[0] );

    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );
      ss.bind( isa );

      selector = selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if (key.isAcceptable()) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );

            User user = new User();
            users.put(sc,user);

            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput( sc );

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
              }

            } catch( IOException ie ) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch( IOException ie2 ) { System.out.println( ie2 ); }

              System.out.println( "Closed "+sc );
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }

  static private void message_all_users(String message,String room) throws IOException{
    buffer.clear();
    buffer.put((message + "\n").getBytes());
    buffer.flip();

    for(SocketChannel sc:users.keySet()){
      if(users.get(sc).getRoom().equals(room)){
        while(buffer.hasRemaining()){
          sc.write(buffer);
        }
      }
      buffer.rewind();
    }

  }

  static private void message_user(String message, SocketChannel sc) throws IOException{

    buffer.clear();
    buffer.put((message + "\n").getBytes());
    buffer.flip();

    while(buffer.hasRemaining()) {
      sc.write(buffer);
    }

  }

  static private boolean checkCommand(SocketChannel sc, String input) throws IOException{
    String[] words = input.split(" ");
    String initial = words[0];
    if(words[0].charAt(0) == '/') {
      if(words[0].length() != 1){
        if(words[0].charAt(1) != '/') {
          switch(initial){
            case "/nick":
              if(words.length == 2) {
                for(SocketChannel tempSC : users.keySet()) {
                  if (users.get(tempSC).getNickname().equals(words[1])) {
                    message_user("ERROR", sc);
                  }
                }
                message_user("OK",sc);
                if(users.get(sc).getState().equals("init")){
                  users.get(sc).setState("outside");
                }
                if(users.get(sc).getState().equals("inside")){
                  message_all_users("NEWNICK " + users.get(sc).getNickname() + " " + words[1], users.get(sc).getRoom());
                }
                users.get(sc).setNickname(words[1]);
              }
              else {
                message_user("ERROR", sc);
              }
              return true;
            case "/join":
              if(words.length == 2){
                if(words[1].equals(users.get(sc).getRoom())){
                  message_user("ERROR",sc);
                  return true;
                }
                if(users.get(sc).getState().equals("init")){
                  message_user("ERROR", sc);
                }
                else{
                  users.get(sc).setState("inside");
                  String Leaving_Room = users.get(sc).getRoom();
                  users.get(sc).setRoom(words[1]);
                  if(Leaving_Room != ""){
                    if(users.get(sc).getState().equals("inside")){
                      message_all_users("LEFT " + users.get(sc).getNickname(), Leaving_Room);
                    }
                  }
                  message_user("OK", sc);
                  message_all_users("JOINED " + users.get(sc).getNickname(), words[1]);
                }
              }
              else {
                message_user("ERROR", sc);
              }
              return true;
            case "/leave":
              if(words.length == 1){
                if(users.get(sc).getState().equals("inside")){
                  users.get(sc).setState("outside");
                  String Leaving_Room = users.get(sc).getRoom();
                  users.get(sc).setRoom("");
                  message_all_users("LEFT " + users.get(sc).getNickname(), Leaving_Room);
                  message_user("OK",sc);
                }
                else{
                  message_user("ERROR",sc);
                }
              }
              else{
                message_user("ERROR",sc);
              }
              return true;
            case "/bye":
              if(words.length == 2){
                message_user("BYE",sc);
                if(users.get(sc).getState().equals("inside")){
                  message_all_users("LEFT " + users.get(sc).getNickname(), users.get(sc).getRoom());
                }
                return false;
              }
              else{
                message_user("ERROR",sc);
              }
            case "/priv":
              if(words.length == 3){
                for(SocketChannel tempSC:users.keySet()){
                  if(users.get(tempSC).getNickname().equals(words[1])){
                    message_user("PRIVATE " + users.get(sc).getNickname() + " " + words[2],tempSC);
                    message_user("OK", sc);
                    return true;
                  }
                }
              }
              message_user("ERROR", sc);
              return true;
            default:
              message_user("ERROR",sc);
              return true;
          }
        }
        else{
          input = input.substring(1);
        }
      }
    }
    if(users.get(sc).getState().equals("inside")){
      message_all_users("MESSAGE " + users.get(sc).getNickname() + " " + input, users.get(sc).getRoom());
    }
    return true;
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput( SocketChannel sc ) throws IOException {


    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit()==0) return false;

    String receivedMessage = decoder.decode(buffer).toString();
    users.get(sc).setBuffer(users.get(sc).getBuffer() + receivedMessage);

    if (receivedMessage.charAt(receivedMessage.length() - 1) != '\n') return false;

    String[] userInput = users.get(sc).getBuffer().split("\n");
    users.get(sc).setBuffer("");

    for (String input:userInput){
      return checkCommand(sc,input);
    }

    // Decode and print the message to stdout
    //String message = decoder.decode(buffer).toString();
    //System.out.print( message );

    return true;
  }
}
