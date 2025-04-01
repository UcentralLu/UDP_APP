package org.vinni.servidor.gui;

import org.vinni.dto.MiDatagrama;

import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrincipalSrv extends JFrame {

    private final int PORT = 12345;
    private final String MULTICAST_GROUP = "230.0.0.1";
    private final int MULTICAST_PORT = 12346;

    public PrincipalSrv() {
        initComponents();
        this.mensajesTxt.setEditable(false);
    }

    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new JButton();
        jLabel1 = new JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(evt -> bIniciarActionPerformed(evt));
        getContentPane().add(bIniciar);
        bIniciar.setBounds(150, 50, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR UDP");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 160, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 150, 500, 120);

        setSize(new java.awt.Dimension(570, 320));
        setLocationRelativeTo(null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalSrv().setVisible(true));
    }

    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciar();
    }

    public void iniciar() {
        mensajesTxt.append("Servidor UDP iniciado en el puerto " + PORT + "\n");
        byte[] buf = new byte[6553];

        new Thread(() -> {
            try (DatagramSocket socketudp = new DatagramSocket(PORT)) {
                this.bIniciar.setEnabled(false);

                while (true) {
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    socketudp.receive(dp);
                    String receivedData = new String(dp.getData(), 0, dp.getLength()).trim();

                    if (receivedData.startsWith("FILE:")) {
                        // Se envía el paquete inicial (información del archivo) y luego se distribuyen los fragmentos
                        distribuirArchivo(socketudp, dp);
                    } else {
                        distribuirMensaje(receivedData);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    /**
     * Método para distribuir el archivo recibido por multicast.
     * Utiliza el paquete inicial recibido (información del archivo) y, a continuación,
     * reenvía todos los fragmentos recibidos hasta encontrar "EOF".
     */
    private void distribuirArchivo(DatagramSocket socket, DatagramPacket fileInfoPacket) {
        try (MulticastSocket multicastSocket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            // Enviar la información del archivo por multicast
            DatagramPacket infoMulticast = new DatagramPacket(
                    fileInfoPacket.getData(), fileInfoPacket.getLength(), group, MULTICAST_PORT);
            multicastSocket.send(infoMulticast);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket fragmentPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(fragmentPacket);
                String fragment = new String(fragmentPacket.getData(), 0, fragmentPacket.getLength()).trim();
                if (fragment.equals("EOF")) {
                    // Enviar el EOF por multicast y salir del bucle
                    DatagramPacket eofPacket = new DatagramPacket("EOF".getBytes(), "EOF".length(), group, MULTICAST_PORT);
                    multicastSocket.send(eofPacket);
                    break;
                }
                // Enviar cada fragmento recibido por multicast
                DatagramPacket multicastFragment = new DatagramPacket(
                        fragmentPacket.getData(), fragmentPacket.getLength(), group, MULTICAST_PORT);
                multicastSocket.send(multicastFragment);
            }
            mensajesTxt.append("Archivo retransmitido a los clientes.\n");
        } catch (IOException ex) {
            Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Método para distribuir mensajes normales por multicast.
     */
    private void distribuirMensaje(String mensaje) {
        try (MulticastSocket multicastSocket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            byte[] mensajeBytes = mensaje.getBytes();
            DatagramPacket mensajePacket = new DatagramPacket(mensajeBytes, mensajeBytes.length, group, MULTICAST_PORT);
            multicastSocket.send(mensajePacket);
            mensajesTxt.append("Mensaje distribuido a todos los clientes.\n");
        } catch (IOException ex) {
            Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Variables declaration - do not modify
    private JButton bIniciar;
    private JLabel jLabel1;
    private JTextArea mensajesTxt;
    private JScrollPane jScrollPane1;
}
