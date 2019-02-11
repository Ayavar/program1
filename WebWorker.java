/**
 * Web worker: an object of this class executes in its own new thread
 * to receive and respond to a single HTTP request. After the constructor
 * the object executes on its "run" method, and leaves when it is done.
 *
 * One WebWorker object is only responsible for one client connection. 
 * This code uses Java threads to parallelize the handling of clients:
 * each WebWorker runs in its own thread. This means that you can essentially
 * just think about what is happening on one client at a time, ignoring 
 * the fact that the entirety of the webserver execution might be handling
 * other clients, too. 
 *
 * This WebWorker class (i.e., an object of this class) is where all the
 * client interaction is done. The "run()" method is the beginning -- think
 * of it as the "main()" for a client interaction. It does three things in
 * a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it
 * writes out some HTML content for the response content. HTTP requests and
 * responses are just lines of text (in a very particular format). 
 *
 **/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{
  String location;
  String loc;
  
  private Socket socket;
  
  /**
   * Constructor: must have a valid open socket
   **/
  public WebWorker(Socket s)
  {
    socket = s;
  }
  
  /**
   * Worker thread starting point. Each worker handles just one HTTP 
   * request and then returns, which destroys the thread. This method
   * assumes that whoever created the worker created it with a valid
   * open socket object.
   **/
  public void run()
  {
    System.err.println("Handling connection...");
    try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
      
      System.out.println(fileExists(location));
      //Checks to see if the user is looking for a file (Example: http://localhost:8080/test.html).
      if (location!=null)
      {
        //If the file is found it will print the file contents.
        if(fileExists(location))
        {
          writeHTTPHeader(os,"text/html");
          writeFileContent(os);
        }
        //If the user is looking for http://localhost:8080/<cs371server> it will print the custom text message.
        else if (location.equals("/%3Ccs371server%3E"))
        {
          writeHTTPHeader(os,"text/html");
          writeCustomContent(os);
        }
        //If the user is looking for http://localhost:8080/<cs371date> it will print the time and date.
        else if (location.equals("/%3Ccs371date%3E"))
        {
          writeHTTPHeader(os,"text/html");
          writeDateContent(os);
        }
        //If the user is not looking for anything (Example: http://localhost:8080/) it will print the default message
        //"My web server works!."
        else if (location.equals("/"))
        {
          writeHTTPHeader(os,"text/html");
          writeContent(os);
        }
        //If the file is not found it will print a custom not found webside.
        else
        {
          write404Header(os,"text/html");
        }
      }
      //For any other requests the program will go to the custom Not Found webside.
      else
      {
        write404Header(os,"text/html");
      }
      os.flush();
      socket.close();
    } catch (Exception e) {
      System.err.println("Output error: "+e);
    }
    System.err.println("Done handling connection.");
    return;
  }
  
  //This method will return a true or false boolean, after looking for the file, depending on the file existing or not existing.
  private boolean fileExists (String location2)
  {
    location2 = (System.getProperty("user.dir")+(location2)).trim();
    loc=location2;
    System.out.println(location2);
    File file = new File (location2);
    return file.exists() && !file.isDirectory() && file.isFile();
  }
  
  /**
   * Read the HTTP request header.
   **/
  private void readHTTPRequest(InputStream is)
  {
    String line;
    String lineCopy;
    BufferedReader r = new BufferedReader(new InputStreamReader(is));
    while (true) {
      try {
        while (!r.ready()) Thread.sleep(1);
        line = r.readLine();
        //line is copied to a new string "lineCopy" to prevent damageing the source "line."
        lineCopy=line;
        
        System.out.println(lineCopy);
        System.err.println("Request line: ("+lineCopy+")");
        
        //Looks for the line containing "GET", to see what the user is looking for.
        if ((lineCopy.contains("GET") || lineCopy.contains("%3Ccs371date%3E") || lineCopy.contains("%3Ccs371server%3E")))
        {
          //After the GET line is found it will remove "GET and "HTTP/1.1" form the line, leaving behide only the name
          //of the file the user is looking for.
          lineCopy = lineCopy.substring(3);
          String[] lineCopyArray = lineCopy.split(" ");
          String lineCopy3 = lineCopyArray[1];
          location=lineCopy3;
        }
        
        else if (lineCopy.length()==0)
        {
          //System.err.println("Request line: ("+Error+404+")");
          break;
        }
        
        
      } catch (Exception e) {
        System.err.println("Request error: "+e);
        break;
      }
    }

    return;
  }
  
  /**
   * Write the HTTP header lines to the client network connection.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   **/
  private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
  {
    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    os.write("HTTP/1.1 200 OK\n".getBytes());
    os.write("Date: ".getBytes());
    os.write((df.format(d)).getBytes());
    os.write("\n".getBytes());
    os.write("Server: Jon's very own server\n".getBytes());
    //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
    //os.write("Content-Length: 438\n".getBytes()); 
    os.write("Connection: close\n".getBytes());
    os.write("Content-Type: ".getBytes());
    os.write(contentType.getBytes());
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
    return;
  }
  
  //This method will print "Not Found" if the file is not found.
  private void write404Header(OutputStream os, String contentType) throws Exception
  {
    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    os.write("HTTP/1.1 404 not found\n".getBytes());
    os.write("Date: ".getBytes());
    os.write((df.format(d)).getBytes());
    os.write("\n".getBytes());
    os.write("Server: Jon's very own server\n".getBytes());
    //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
    //os.write("Content-Length: 438\n".getBytes()); 
    os.write("Connection: close\n".getBytes());
    os.write("Content-Type: ".getBytes());
    os.write(contentType.getBytes());
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
    
    os.write("<html><head>\n".getBytes());
    os.write("<title>404 Not Found</title>\n".getBytes());
    os.write("</head><body>\n".getBytes());
    os.write("<h1>Not Found</h1>\n".getBytes());
    os.write("<p>The requested URL http://localhost:8080".getBytes());
    os.write(location.getBytes());
    os.write(" was not found on this server.".getBytes());
    os.write("</body></html>\n".getBytes());
    
    //os.write("<html><head></head><body>\n".getBytes());
    //os.write("<h3>Error 404</h3>\n".getBytes());
    //os.write("</body></html>\n".getBytes());
    return;
  }
  
  
  /**
   * Write the data content to the client network connection. This MUST
   * be done after the HTTP header has been written out.
   * @param os is the OutputStream object to write to
   **/
  //This method prints "My web server works!" when the user is not looking for any files
  //(Example: http://localhost:8080).
  private void writeContent(OutputStream os) throws Exception
  {
    os.write("<html><head></head><body>\n".getBytes());
    os.write("<h3>My web server works!</h3>\n".getBytes());
    os.write("</body></html>\n".getBytes());
  }
  //This method prints custom message when the user types http://localhost:8080/<cs371server>.
  private void writeCustomContent(OutputStream os) throws Exception
  {
    os.write("<html><head></head><body>\n".getBytes());
    os.write("<h3>Welcome to Jonathan's server!</h3>\n".getBytes());
    os.write("</body></html>\n".getBytes());
  }
  
    //This method prints the date and time when the user types http://localhost:8080/<cs371date>.
    private void writeDateContent(OutputStream os) throws Exception
  {
      Date d = new Date();
      DateFormat df = DateFormat.getDateTimeInstance();
      df.setTimeZone(TimeZone.getTimeZone("GMT"));
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Jonathan's very own server\n".getBytes());
      //os.write("Connection: close\n".getBytes());
      //os.write("Content-Type: ".getBytes());
      //os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
  }
  
  //This method will print the contents of the desired file. 
   private void writeFileContent(OutputStream os) throws Exception
   {
     Scanner input = new Scanner (new File(loc));
     while(input.hasNextLine())
     {
          os.write(input.nextLine().getBytes());
     }
  }
  
} // end class
