import java.io.BufferedReader;
import java.io.DataOutputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.HashMap;

class HTTPServer {
  public static void main(String[] args) throws Exception {
    try {
      int port = 8080;
      ServerSocket serverSocket = new ServerSocket(port);
      System.out.println("Server is running on localhost:" + port);
      
      while (true) {
        Socket socket = serverSocket.accept();
        new HTTPServerThread(socket).start();
      }
    } catch (Exception e) {System.out.println("Error in Multi-Thread: " + e.getMessage());}
  }
}

class HTTPServerThread extends Thread {
  Socket socket;
  HTTPServerThread(Socket socket) {this.socket = socket;}

  public void log(String[] req, String statusCode) {
    File file = new File("log.txt");

    try {
      FileWriter writer = new FileWriter(file, true);
      writer.write(socket.getRemoteSocketAddress().toString() + "\t");
      writer.write(new SimpleDateFormat("[dd/MMM/yyyy:HH:mm:ss]").format(new Date().getTime()) + "\t");
      writer.write(req[1] + "\t");
      writer.write(statusCode + "\n");
      writer.close();
    } catch (Exception e) {System.out.println(e);}
  }

  public void run() {
    try {
      InputStream input = socket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      DataOutputStream output = new DataOutputStream(socket.getOutputStream());

      String line = reader.readLine();
      String[] req = line.split(" ");
      HashMap<String, String> header = new HashMap<>();
      
      do {
        line = reader.readLine();
        String[] arr = line.split(": ");
        if (arr.length == 1)
          break;
        header.put(arr[0], arr[1]);
      } while (!line.equals("\n"));

      if (req[0].equals("GET") || req[0].equals("HEAD")) {
        String path = req[1];
        String filename = path.charAt(0) == '\\' ? path.substring(1) : path;

        File file = new File("src", filename);
        String statusCode;
        FileInputStream fileStream;
        
        if (header.containsKey("If-Modified-Since")) {
          Date checkDate = new Date(header.get("If-Modified-Since"));
          Date fileDate = new Date(file.lastModified());
          if (checkDate.compareTo(fileDate) >= 0) {
            output.writeBytes("HTTP/1.1 304 Not Modified\r\n");
            log(req, "304");
            reader.close();
            socket.close();
            return;
          }
        }

        try {
          fileStream = new FileInputStream(file); 
          statusCode = "200";
        } catch (FileNotFoundException e) {
          file = new File("src", "404.html"); 
          statusCode = "404";
        } finally {fileStream = new FileInputStream(file);}

        byte[] fileInBytes = new byte[(int) file.length()];
        fileStream.read(fileInBytes);

        output.writeBytes("HTTP/1.1 " + statusCode + "\r\n");
        SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
        output.writeBytes("Last-Modified: " + dateFormat.format(file.lastModified()) + " GMT\r\n");
        output.writeBytes("\r\n");
        
        if (req[0].equals("GET"))
          output.write(fileInBytes, 0, (int) file.length());
        
        log(req, statusCode);
        fileStream.close();
        reader.close();
        socket.close();
      } 
      else {
        output.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
        log(req, "400");
        socket.close();
      }
    } catch (Exception e) {System.out.println("Error in server: " + e + " " + e.getMessage());}
  }
}