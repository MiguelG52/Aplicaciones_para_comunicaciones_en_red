import java.io.Serializable;

class Fragmento implements Serializable {
    private String idArchivo;   // Identificador único del archivo
    private int indice;         // Índice del fragmento
    private byte[] datos;       // Datos del fragmento
    private int tamanio;        // Tamaño real de los datos
    private boolean ultimo;     // Indica si es el último fragmento

    public Fragmento(String idArchivo, int indice, byte[] datos, int tamanio, boolean ultimo) {
        this.idArchivo = idArchivo;
        this.indice = indice;
        this.datos = datos != null ? datos.clone() : null;
        this.tamanio = tamanio;
        this.ultimo = ultimo;
    }

    // Getters
    public String getIdArchivo() {
        return idArchivo;
    }

    public int getIndice() {
        return indice;
    }

    public byte[] getDatos() {
        return datos;
    }

    public int getTamanio() {
        return tamanio;
    }

    public boolean isUltimo() {
        return ultimo;
    }

    // Setters
    public void setIdArchivo(String idArchivo) {
        this.idArchivo = idArchivo;
    }

    public void setIndice(int indice) {
        this.indice = indice;
    }

    public void setDatos(byte[] datos) {
        this.datos = datos;
    }

    public void setTamanio(int tamanio) {
        this.tamanio = tamanio;
    }

    public void setUltimo(boolean ultimo) {
        this.ultimo = ultimo;
    }
}
