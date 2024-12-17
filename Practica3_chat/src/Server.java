import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final String MULTICAST_ADDRESS = "230.0.0.0";
    private static final int MULTICAST_PORT = 12345;

    public static void main(String[] args) {
        try {
            int pto = 8081;
            DatagramSocket s = new DatagramSocket(pto);
            InetAddress multicastGroup = InetAddress.getByName(MULTICAST_ADDRESS);
            System.out.println("Servidor iniciado. Esperando clientes...");

            while (true) {
                DatagramPacket p = new DatagramPacket(new byte[65535], 65535);
                s.receive(p);

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(p.getData()));
                Objeto recibido = (Objeto) ois.readObject();

                InetAddress clienteIP = p.getAddress();
                int clientePort = p.getPort();

                switch (recibido.getAccion()) {
                    case "listar" -> {
                        File carpetaServidor = new File("C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\Practica3_chat\\server");
                        String[] archivosServidor = carpetaServidor.list();

                        String listaArchivos = (archivosServidor != null && archivosServidor.length > 0)
                                ? String.join("\n", archivosServidor)
                                : "No hay archivos disponibles en el servidor.";

                        Objeto respuesta = new Objeto("listar", listaArchivos, null, null);
                        enviarObjeto(s, respuesta, clienteIP, clientePort);
                    }

                    case "subir" -> {
                        String nombreArchivo = recibido.getNombreArchivo();
                        File archivo = new File("C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\Practica3_chat\\server\\" + nombreArchivo);

                        try (FileOutputStream fos = new FileOutputStream(archivo, true)) {
                            fos.write(recibido.getArchivo());
                        }

                        // Enviar ACK al cliente
                        Objeto ack = new Objeto("ACK", "ACK:" + recibido.getIndiceFragmento(), null, null);
                        enviarObjeto(s, ack, clienteIP, clientePort);

                        if (recibido.isUltimoFragmento()) {
                            System.out.println("Archivo " + nombreArchivo + " recibido completamente.");

                            // Notificar a los clientes en el grupo multicast
                            String mensaje = "<Archivo> El archivo '" + nombreArchivo + "' ha sido subido al servidor.";
                            enviarMulticast(mensaje, multicastGroup);
                        }
                    }

                    case "descargar" -> {
                        String nombreArchivo = recibido.getNombreArchivo();
                        File archivo = new File("C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\Practica3_chat\\server\\" + nombreArchivo);

                        if (!archivo.exists()) {
                            System.out.println("Archivo solicitado no existe: " + nombreArchivo);
                            continue;
                        }

                        try (FileInputStream fis = new FileInputStream(archivo)) {
                            byte[] buffer = new byte[60000];
                            int bytesLeidos, indice = 0;

                            while ((bytesLeidos = fis.read(buffer)) != -1) {
                                boolean ackRecibido = false;

                                while (!ackRecibido) {
                                    Objeto fragmento = new Objeto(
                                            "descargar",
                                            "Enviando fragmento " + indice,
                                            Arrays.copyOf(buffer, bytesLeidos),
                                            nombreArchivo
                                    );
                                    fragmento.setIndiceFragmento(indice);
                                    fragmento.setUltimoFragmento(bytesLeidos < buffer.length);

                                    enviarObjeto(s, fragmento, clienteIP, clientePort);

                                    try {
                                        DatagramPacket ackPacket = new DatagramPacket(new byte[65535], 65535);
                                        s.receive(ackPacket);

                                        ObjectInputStream ackOis = new ObjectInputStream(new ByteArrayInputStream(ackPacket.getData()));
                                        Objeto ack = (Objeto) ackOis.readObject();
                                        ackRecibido = ack.getMensaje().equals("ACK:" + indice);
                                    } catch (SocketTimeoutException e) {
                                        System.out.println("Timeout esperando ACK para fragmento " + indice + ". Retransmitiendo...");
                                    }
                                }
                                indice++;
                            }
                        }
                        System.out.println("Archivo enviado: " + nombreArchivo);
                    }

                    default -> System.out.println("Acción desconocida: " + recibido.getAccion());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enviarObjeto(DatagramSocket socket, Objeto objeto, InetAddress clienteIP, int clientePort) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(objeto);
            oos.flush();

            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, clienteIP, clientePort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void enviarMulticast(String mensaje, InetAddress multicastGroup) {
        try (MulticastSocket multicastSocket = new MulticastSocket()) {
            byte[] buffer = mensaje.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, multicastGroup, MULTICAST_PORT);
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
