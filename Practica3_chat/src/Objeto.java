import java.io.Serializable;

public class Objeto implements Serializable {
    private String accion;         // Acción a realizar: "listar", "subir", "descargar"
    private String mensaje;        // Mensaje de estado
    private byte[] archivo;        // Contenido del archivo (en caso de que aplique)
    private String nombreArchivo;  // Nombre del archivo
    private int indiceFragmento;   // Índice del fragmento (en caso de ser un fragmento)
    private boolean ultimoFragmento; // Indica si es el último fragmento del archivo

    public Objeto(String accion, String mensaje, byte[] archivo, String nombreArchivo) {
        this.accion = accion;
        this.mensaje = mensaje;
        this.archivo = archivo != null ? archivo.clone() : null;
        this.nombreArchivo = nombreArchivo;
        this.indiceFragmento = -1;
        this.ultimoFragmento = false;
    }

    // Getters
    public String getAccion() {
        return accion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public byte[] getArchivo() {
        return archivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public int getIndiceFragmento() {
        return indiceFragmento;
    }

    public boolean isUltimoFragmento() {
        return ultimoFragmento;
    }

    // Setters
    public void setAccion(String accion) {
        this.accion = accion;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setArchivo(byte[] archivo) {
        this.archivo = archivo != null ? archivo.clone() : null;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public void setIndiceFragmento(int indiceFragmento) {
        this.indiceFragmento = indiceFragmento;
    }

    public void setUltimoFragmento(boolean ultimoFragmento) {
        this.ultimoFragmento = ultimoFragmento;
    }
}
