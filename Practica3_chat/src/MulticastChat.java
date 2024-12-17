import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

public class MulticastChat {
    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int MULTICAST_PORT = 12345;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8081;
    private String username;
    private MulticastSocket multicastSocket;
    private DatagramSocket socket;
    private InetAddress group;
    private JTextArea chatArea;
    private DefaultListModel<String> fileListModel;
    private static final String TYPING_INDICATOR = " está escribiendo..."; // Mensaje de escritura
    private boolean isTyping = false; // Indicador de si el usuario está escribiendo

    public MulticastChat() {
        setupUI();
    }

    private void setupUI() {
        JFrame frame = new JFrame("Multicast Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);

        JPanel panel = new JPanel(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Enviar");
        JButton uploadButton = new JButton("Subir Archivo");
        JButton listFilesButton = new JButton("Ver Archivos");
        JButton downloadButton = new JButton("Descargar Archivo");
        JButton sendEmojiButton = new JButton("Enviar Emoji");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel filePanel = new JPanel(new FlowLayout());
        
        filePanel.add(listFilesButton);
        filePanel.add(downloadButton);
        filePanel.add(uploadButton);
        filePanel.add(sendEmojiButton);
    


        panel.add(inputPanel, BorderLayout.SOUTH);
        panel.add(filePanel, BorderLayout.NORTH);

        frame.add(panel);

        // Prompt for username
        username = JOptionPane.showInputDialog(frame, "Ingresa tu nombre:");
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "El nombre de usuario es requerido.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        listFilesButton.addActionListener(e -> listFiles());
        downloadButton.addActionListener(e -> downloadFile());
        sendEmojiButton.addActionListener(e -> sendEmoji());

        sendButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                sendMessage("<"+username+"><Mensaje> " + message);
                inputField.setText("");
            }
        });

        uploadButton.addActionListener(e -> uploadFile());

        frame.setVisible(true);
        joinChat();
    }

    private void joinChat() {
        try {
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            socket = new DatagramSocket();
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(this::receiveMessages);

            sendMessage("<Login> " + username + " se unió al chat.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                
                chatArea.append(message + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void listFiles() {
        try {
            Objeto request = new Objeto("listar", null, null, null);
            sendObjectToServer(request);

            Objeto response = receiveObjectFromServer();
            JOptionPane.showMessageDialog(null, response.getMensaje(), "Archivos del Servidor", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void sendEmoji() {
        // Lista de "emojis" tipo ASCII
        String[] asciiEmojis = {
            ":-)",   // Sonrisa
            ":-D",   // Risa
            ":-(",   // Tristeza
            ":-O",   // Sorprendido
            ";-)",   // Guiño
            ":-|",   // Neutral
            ":-((",  // Llorando
            ":P",    // Saca la lengua
            ":-/",   // Dudoso
            "<3"     // Corazón
        };
    
        String selectedEmoji = (String) JOptionPane.showInputDialog(
            null,
            "Selecciona un emoji:",
            "Seleccionar Emoji",
            JOptionPane.PLAIN_MESSAGE,
            null,
            asciiEmojis,
            asciiEmojis[0] // Valor por defecto
        );
    
        if (selectedEmoji != null && !selectedEmoji.isEmpty()) {
            sendMessage("<" + username + "><Emoji> " + selectedEmoji); 
        }
    }
    
    

    private void downloadFile() {
        String fileName = JOptionPane.showInputDialog("Ingrese el nombre del archivo a descargar:");
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
    
        try {
            Objeto request = new Objeto("descargar", "", null, fileName);
            sendObjectToServer(request);
    
            // Ruta de la carpeta del usuario
            String userDirectoryPath = "C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\Practica3_chat\\client\\" + username;
            File userDirectory = new File(userDirectoryPath);
    
            // Crear la carpeta del usuario si no existe
            if (!userDirectory.exists()) {
                boolean dirCreated = userDirectory.mkdirs();
                if (!dirCreated) {
                    JOptionPane.showMessageDialog(null, "No se pudo crear la carpeta para el usuario.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
    
            // Ruta completa para el archivo
            File file = new File(userDirectory, fileName);
    
            try (FileOutputStream fos = new FileOutputStream(file)) {
                Objeto fragment;
                int fragmentIndex = 0;
                boolean lastFragment = false;
                while (!lastFragment) {
                    fragment = receiveObjectFromServer();
                    fos.write(fragment.getArchivo());
    
                    // Enviar ACK después de recibir el fragmento
                    Objeto ack = new Objeto("ACK", "ACK:" + fragmentIndex, null, null);
                    sendObjectToServer(ack);
    
                    lastFragment = fragment.isUltimoFragmento();
                    fragmentIndex++;
                }
    
                JOptionPane.showMessageDialog(null, "Archivo descargado con éxito.", "Descarga", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            System.out.print(file.getName());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[60000];
                int bytesRead, index = 0;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    Objeto fragment = new Objeto("subir", file.getName(), Arrays.copyOf(buffer, bytesRead), file.getName());
                    fragment.setIndiceFragmento(index);
                    fragment.setUltimoFragmento(bytesRead < buffer.length);

                    sendObjectToServer(fragment);

                    Objeto ack = receiveObjectFromServer();
                    if (!ack.getMensaje().equals("ACK:" + index)) {
                        throw new IOException("ACK no recibido para el fragmento: " + index);
                    }

                    index++;
                }
                JOptionPane.showMessageDialog(null, "Archivo subido exitosamente.", "Subida", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendObjectToServer(Objeto objeto) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(objeto);
        oos.flush();

        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
        socket.send(packet);
    }

    private Objeto receiveObjectFromServer() throws IOException, ClassNotFoundException {
        DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
        socket.receive(packet);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
        return (Objeto) ois.readObject();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MulticastChat::new);
    }
}
