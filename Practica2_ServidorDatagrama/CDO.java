import java.io.*;
import java.net.*;
import java.util.*;

public class CDO {
    public static void main(String[] args) {
        try {
            String host = "127.0.0.1";
            int pto = 8081;
            InetAddress dst = InetAddress.getByName(host);
            DatagramSocket cl = new DatagramSocket();
            cl.setSoTimeout(2000); // Timeout de 2 segundos para evitar bloqueos
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\n1. Listar archivos del cliente");
                System.out.println("2. Subir archivo al servidor");
                System.out.println("3. Descargar archivo del servidor");
                System.out.println("4. Ver archivos del servidor");
                System.out.println("5. Salir");
                System.out.println("Seleccione una acción:");

                int opcion = sc.nextInt();
                sc.nextLine();

                if (opcion == 5) break;

                Objeto objeto = null;

                switch (opcion) {
                    case 1 -> {
                        File carpetaCliente = new File("C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\P2_java\\client");
                        String[] archivosCliente = carpetaCliente.list();
                        if (archivosCliente != null) {
                            System.out.println("Archivos en la carpeta cliente:");
                            for (String archivo : archivosCliente) {
                                System.out.println(archivo);
                            }
                        } else {
                            System.out.println("La carpeta cliente está vacía.");
                        }
                        continue;
                    }

                    case 2 -> {
                        System.out.print("Ingrese el nombre del archivo a subir: ");
                        String archivoSubir = sc.nextLine();
                        File archivo = new File("C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\P2_java\\client\\" + archivoSubir);

                        if (!archivo.exists()) {
                            System.out.println("El archivo no existe.");
                            continue;
                        }

                        try (FileInputStream fis = new FileInputStream(archivo)) {
                            byte[] buffer = new byte[60000];
                            int bytesLeidos, indice = 0;

                            while ((bytesLeidos = fis.read(buffer)) != -1) {
                                boolean ackRecibido = false;
                                while (!ackRecibido) {
                                    Objeto fragmento = new Objeto(
                                            "subir",
                                            "Enviando fragmento " + indice,
                                            Arrays.copyOf(buffer, bytesLeidos),
                                            archivoSubir
                                    );
                                    fragmento.setIndiceFragmento(indice);
                                    fragmento.setUltimoFragmento(bytesLeidos < buffer.length);

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                                    oos.writeObject(fragmento);
                                    oos.flush();

                                    byte[] data = baos.toByteArray();
                                    DatagramPacket p = new DatagramPacket(data, data.length, dst, pto);
                                    cl.send(p);

                                    try {
                                        DatagramPacket ackPacket = new DatagramPacket(new byte[65535], 65535);
                                        cl.receive(ackPacket);

                                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(ackPacket.getData()));
                                        Objeto ack = (Objeto) ois.readObject();
                                        ackRecibido = ack.getMensaje().equals("ACK:" + indice);
                                    } catch (SocketTimeoutException e) {
                                        System.out.println("Timeout esperando ACK para fragmento " + indice + ". Retransmitiendo...");
                                    }
                                }
                                indice++;
                            }
                        }
                        System.out.println("Archivo subido exitosamente.");
                    }

                    case 3 -> {
                        System.out.print("\nIngrese el nombre del archivo a descargar: ");
                        String archivoDescargar = sc.nextLine();
                        objeto = new Objeto("descargar", "Descargando archivo", null, archivoDescargar);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(objeto);
                        oos.flush();
                        byte[] b = baos.toByteArray();

                        DatagramPacket p = new DatagramPacket(b, b.length, dst, pto);
                        cl.send(p);

                        TreeMap<Integer, byte[]> archivoRecibido = new TreeMap<>();
                        boolean ultimoFragmentoRecibido = false;

                        while (!ultimoFragmentoRecibido) {
                            try {
                                DatagramPacket p1 = new DatagramPacket(new byte[65535], 65535);
                                cl.receive(p1);

                                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(p1.getData()));
                                Objeto fragmento = (Objeto) ois.readObject();

                                archivoRecibido.put(fragmento.getIndiceFragmento(), fragmento.getArchivo());
                                ultimoFragmentoRecibido = fragmento.isUltimoFragmento();

                                // Enviar ACK
                                Objeto ack = new Objeto("ACK", "ACK:" + fragmento.getIndiceFragmento(), null, null);
                                ByteArrayOutputStream baosAck = new ByteArrayOutputStream();
                                ObjectOutputStream oosAck = new ObjectOutputStream(baosAck);
                                oosAck.writeObject(ack);
                                oosAck.flush();

                                byte[] ackData = baosAck.toByteArray();
                                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, p1.getAddress(), p1.getPort());
                                cl.send(ackPacket);
                            } catch (SocketTimeoutException e) {
                                System.out.println("Timeout esperando fragmentos. Continuando...");
                            }
                        }

                        File carpetaDescargas = new File("C:\\Users\\Migue\\Documents\\GitHub\\Aplicaciones_para_comunicaciones_en_red\\P2_java\\client\\descargas");
                        if (!carpetaDescargas.exists()) {
                            carpetaDescargas.mkdirs();
                        }

                        File archivoFinal = new File(carpetaDescargas, archivoDescargar);
                        try (FileOutputStream fos = new FileOutputStream(archivoFinal)) {
                            for (byte[] fragmento : archivoRecibido.values()) {
                                fos.write(fragmento);
                            }
                        }
                        System.out.println("\nArchivo descargado en: client\\descargas\\" + archivoDescargar);
                    }

                    case 4 -> {
                        objeto = new Objeto("listar", "Listando contenido del servidor", null, null);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(objeto);
                        oos.flush();
                        byte[] b = baos.toByteArray();

                        DatagramPacket p = new DatagramPacket(b, b.length, dst, pto);
                        cl.send(p);

                        DatagramPacket p1 = new DatagramPacket(new byte[65535], 65535);
                        cl.receive(p1);
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(p1.getData()));
                        Objeto respuestaListar = (Objeto) ois.readObject();
                        System.out.println("\nContenido del servidor:");
                        System.out.println(respuestaListar.getMensaje());
                    }

                    default -> System.out.println("\nOpción no válida.");
                }
            }
            cl.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
