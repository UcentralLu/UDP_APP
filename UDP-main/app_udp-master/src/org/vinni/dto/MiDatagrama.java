package org.vinni.dto;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MiDatagrama {
    
    public static DatagramPacket crearDataG(String ip, int puerto, String mensaje){
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            byte[] mensajeB = mensaje.getBytes();
            return new DatagramPacket(mensajeB, mensaje.length(), direccion, puerto);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MiDatagrama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static DatagramPacket crearPaqueteArchivo(String ip, int puerto, byte[] datos, int longitud) {
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            return new DatagramPacket(datos, longitud, direccion, puerto);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MiDatagrama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    // Método modificado para incluir el identificador del remitente (sender)
    public static DatagramPacket crearPaqueteInfoArchivo(String ip, int puerto, String nombreArchivo, long tamanoArchivo, String sender) {
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            String infoArchivo = "FILE:" + nombreArchivo + ":" + tamanoArchivo + ":" + sender;
            byte[] datos = infoArchivo.getBytes();
            return new DatagramPacket(datos, datos.length, direccion, puerto);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MiDatagrama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
