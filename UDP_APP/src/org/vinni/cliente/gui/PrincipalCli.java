package org.vinni.cliente.gui;

import org.vinni.dto.MiDatagrama;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrincipalCli extends JFrame {

    private final int PORT = 12345;
    private final String MULTICAST_GROUP = "230.0.0.1";
    private final int MULTICAST_PORT = 12346;
    private String nombreCliente = "";
    private String directorioCliente;
    // Contador para renombrar archivos recibidos
    private int fileCounter = 0;

    public PrincipalCli() {
        // Solicitar el nombre del cliente antes de iniciar cualquier acción
        solicitarNombre();
        
        // Crear carpeta única para el cliente usando su nombre
        this.directorioCliente = "cliente_" + nombreCliente;
        new File(directorioCliente).mkdirs();

        initComponents();

        this.btEnviar.setEnabled(true);
        this.mensajesTxt.setEditable(false);

        new Thread(this::recibirMensajesMulticast).start();
    }
    
    // Método para solicitar el nombre de usuario (se obliga a ingresar uno válido)
    private void solicitarNombre() {
        do {
            nombreCliente = JOptionPane.showInputDialog(this, "Ingrese su nombre:");
            if (nombreCliente == null || nombreCliente.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Debe ingresar un nombre para continuar.");
            } else {
                nombreCliente = nombreCliente.trim();
            }
        } while (nombreCliente.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        this.setTitle("Cliente - " + nombreCliente);
        jLabel1 = new JLabel();
        jScrollPane1 = new JScrollPane();
        mensajesTxt = new JTextArea();
        mensajeTxt = new JTextField();
        jLabel2 = new JLabel();
        btEnviar = new JButton();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); 
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("CLIENTE UDP : LUING");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(110, 10, 250, 17);

        mensajesTxt.setColumns(20);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(30, 210, 410, 110);

        mensajeTxt.setFont(new java.awt.Font("Verdana", 0, 14));
        getContentPane().add(mensajeTxt);
        mensajeTxt.setBounds(40, 120, 350, 30);

        jLabel2.setFont(new java.awt.Font("Verdana", 0, 14));
        jLabel2.setText("Mensaje:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 90, 120, 30);

        nombreTxt = new JTextField();
        jLabel3 = new JLabel();
        btSeleccionarArchivo = new JButton();
        fileChooser = new JFileChooser();

        jLabel3.setFont(new java.awt.Font("Verdana", 0, 14));
        jLabel3.setText("Nombre:");
        getContentPane().add(jLabel3);
        jLabel3.setBounds(20, 30, 120, 30);

        // Se muestra el nombre ya ingresado (no editable)
        nombreTxt.setFont(new java.awt.Font("Verdana", 0, 14));
        nombreTxt.setText(nombreCliente);
        nombreTxt.setEditable(false);
        getContentPane().add(nombreTxt);
        nombreTxt.setBounds(40, 60, 350, 30);

        btSeleccionarArchivo.setText("Seleccionar Archivo");
        btSeleccionarArchivo.addActionListener(evt -> seleccionarArchivo());
        getContentPane().add(btSeleccionarArchivo);
        btSeleccionarArchivo.setBounds(40, 160, 200, 27);

        btEnviar.setFont(new java.awt.Font("Verdana", 0, 14));
        btEnviar.setText("Enviar");
        btEnviar.addActionListener(evt -> btEnviarActionPerformed(evt));
        getContentPane().add(btEnviar);
        btEnviar.setBounds(327, 160, 120, 27);

        setSize(new java.awt.Dimension(491, 375));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalCli().setVisible(true));
    }

    // Variables de la interfaz
    private JButton btEnviar;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JScrollPane jScrollPane1;
    private JTextArea mensajesTxt;
    private JTextField mensajeTxt;
    private JTextField nombreTxt;
    private JButton btSeleccionarArchivo;
    private JFileChooser fileChooser;
    private File selectedFile;    

    private void btEnviarActionPerformed(java.awt.event.ActionEvent evt) {
        String mensaje = mensajeTxt.getText().trim();
        if (mensaje.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un mensaje para enviar.");
            return;
        }
        enviarMensaje();
    }

    private void seleccionarArchivo() {
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().endsWith(".mp3")) {
                JOptionPane.showMessageDialog(this, "Debe seleccionar un archivo .mp3");
                selectedFile = null;
            }
        }
    }

    private void enviarMensaje() {
        String ip = "127.0.0.1";
        String mensaje = mensajeTxt.getText().trim();

        if (mensaje.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un mensaje para enviar");
            return;
        }

        String contenido = nombreCliente + ": " + mensaje;
        DatagramPacket mensajeDG = MiDatagrama.crearDataG(ip, PORT, contenido);
     
        try (DatagramSocket canal = new DatagramSocket()) {
            canal.send(mensajeDG);
            mensajesTxt.append("Mensaje enviado\n");
    
            if (selectedFile != null) {
                enviarArchivo(canal, ip, selectedFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void enviarArchivo(DatagramSocket canal, String ip, File file) {
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            byte[] buffer = new byte[1024];
    
            // Enviar paquete de información del archivo incluyendo el remitente (nombreCliente)
            DatagramPacket paqueteInfo = MiDatagrama.crearPaqueteInfoArchivo(ip, PORT, file.getName(), file.length(), nombreCliente);
            canal.send(paqueteInfo);
            
            try (FileInputStream fis = new FileInputStream(file)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    DatagramPacket paquete = MiDatagrama.crearPaqueteArchivo(ip, PORT, buffer, bytesRead);
                    canal.send(paquete);
                    Thread.sleep(10);
                }
            }
    
            DatagramPacket paqueteFin = MiDatagrama.crearDataG(ip, PORT, "EOF");
            canal.send(paqueteFin);
    
            mensajesTxt.append("Archivo enviado: " + file.getName() + "\n");
    
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Recepción de mensajes multicast (mensajes y archivos)
    private void recibirMensajesMulticast() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);
            
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String mensaje = new String(packet.getData(), 0, packet.getLength());
                
                if (mensaje.startsWith("FILE:")) {
                    // Llamada al método actualizado que pregunta si se desea aceptar el archivo
                    recibirArchivo(socket, mensaje);
                } else {
                    mensajesTxt.append(mensaje + "\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // Método actualizado para recibir archivo y renombrarlo según el nombre del cliente y un contador
    private void recibirArchivo(MulticastSocket socket, String infoArchivo) throws IOException {
        String[] partes = infoArchivo.split(":");
        // Se esperan al menos 4 partes: FILE, nombreArchivo, tamanoArchivo y sender
        if (partes.length < 4) return;
        
        String fileName = partes[1];
        int fileSize = Integer.parseInt(partes[2]);
        String sender = partes[3];
        
        // Si el remitente es el mismo cliente, se ignora la recepción
        if (sender.equals(nombreCliente)) {
            mensajesTxt.append("Se ignora archivo enviado por mi mismo.\n");
            return;
        }
        
        // Notificar al usuario que se ha recibido un archivo de 'sender'
        int respuesta = JOptionPane.showConfirmDialog(this,
                "El cliente " + sender + " ha enviado el archivo '" + fileName + "'. ¿Desea aceptarlo?",
                "Aceptar archivo", JOptionPane.YES_NO_OPTION);
        
        if (respuesta != JOptionPane.YES_OPTION) {
            mensajesTxt.append("Archivo de " + sender + " rechazado.\n");
            return;
        }
        
        // Incrementar contador para renombrar el archivo
        fileCounter++;
        // Se genera el nuevo nombre usando el nombre del cliente y el contador (ej. Alicia1.mp3)
        String nuevoNombre = nombreCliente + fileCounter + ".mp3";
        File receivedFile = new File(directorioCliente, nuevoNombre);
        
        try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead, totalBytes = 0;
            while (totalBytes < fileSize) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String fragmento = new String(packet.getData(), 0, packet.getLength()).trim();
                if (fragmento.equals("EOF")) break;
                bytesRead = packet.getLength();
                fos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
        }
        mensajesTxt.append("Archivo recibido y guardado como " + nuevoNombre + " en " + directorioCliente + "\n");
    }
    
    // Variables declaración (parte inferior de la clase)
    private JTextField jTextField1;
}
