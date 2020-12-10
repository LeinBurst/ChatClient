import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class ChatClient {

  // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
  JFrame frame = new JFrame("Chat Client");
  private JTextField chatBox = new JTextField();
  private JTextArea chatArea = new JTextArea();
  // --- Fim das variáveis relacionadas coma interface gráfica

  // Se for necessário adicionar variáveis ao objecto ChatClient, devem
  // ser colocadas aqui
  static private final int BUFFER_SIZE = 16384;
  static private SocketChannel sc = null;
  static private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
  static private Charset charset = Charset.forName("UTF8");
  static private CharsetDecoder decoder = charset.newDecoder();

  // Método a usar para acrescentar uma string à caixa de texto
  // * NÃO MODIFICAR *
  public void printMessage(final String message) {
    chatArea.append(message);
  }


  // Construtor
  public ChatClient(String server, int port) throws IOException {

    // Inicialização da interface gráfica --- * NÃO MODIFICAR *
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(chatBox);
    frame.setLayout(new BorderLayout());
    frame.add(panel, BorderLayout.SOUTH);
    frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
    frame.setSize(500, 300);
    frame.setVisible(true);
    chatArea.setEditable(false);
    chatBox.setEditable(true);
    chatBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          newMessage(chatBox.getText());
        } catch (IOException ex) {
        } finally {
          chatBox.setText("");
        }
      }
    });
    frame.addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        chatBox.requestFocus();
      }
    });
    // --- Fim da inicialização da interface gráfica

    // Se for necessário adicionar código de inicialização ao
    // construtor, deve ser colocado aqui
    Thread thread = new Thread();
    thread.run();

    InetSocketAddress isa = new InetSocketAddress(server,port);
    sc = SocketChannel.open(isa);
  }


  // Método invocado sempre que o utilizador insere uma mensagem
  // na caixa de entrada
  public void newMessage(String message) throws IOException {
    buffer.clear();
    sc.write(charset.encode(message + "\n"));
  }


  // Método principal do objecto
  public void run() throws IOException {

    while(true){
      buffer.clear();
      sc.read(buffer);
      buffer.flip();

      if(buffer.limit() == 0) continue;

      String data = decoder.decode(buffer).toString();
      printMessage(data);
    }

  }


  // Instancia o ChatClient e arranca-o invocando o seu método run()
  // * NÃO MODIFICAR *
  public static void main(String[] args) throws IOException {
    ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
    client.run();
  }

}
