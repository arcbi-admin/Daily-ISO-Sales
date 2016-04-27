package dailysalesiso;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

//import javax.activation.*;

public class DailySalesISO
{
     static Connection con;
     static ResultSet rs;
     static PreparedStatement pStatement;
     static final String DBURL = "jdbc:oracle:thin:@alexius.metro.com.ph:1521/mgrmsp";
     static final String DBUSER = "rmsprd";
     static final String DBPASS = "noida123";
     static String date,date2;
     static File file;
    
   public static void main(String [] args) throws SQLException, IOException{
       long startTime = System.nanoTime();
       System.out.println("\n\nDaily ISO Sales");
       ExtractData();
       SendEmail();
       long endTime = System.nanoTime();
       long elapsedTime =   endTime - startTime;
       double elapsedSec = (double)elapsedTime / 1000000000.0;
       System.out.println("Elapsed Time: "+elapsedSec);
       
   }
   
   private static void ExtractData() throws SQLException, IOException{
        System.out.println("Connecting to Database..");
        
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        con = DriverManager.getConnection(DBURL, DBUSER, DBPASS);
        
         getDate();
        System.out.println("Fetching Data..");
        
        String query = "Select A.Store AS STORE, to_char(Trunc(A.Tran_Datetime),'DD-MON-YYYY') AS BUSINESS_DATE, To_Char(A.Tran_Datetime,'HH24:MI:SS AM') AS MY_TIME,"
             +"C.Cust_Id AS CUST_ID, C.Postal_Code AS POSTAL_CODE, B.Tran_Seq_No AS TRAN_SEQ_NO, a.Tran_No AS TRAN_NO, B.Item_Seq_No AS ITEM_SEQ_NO, Substr(Lpad(a.Tran_No,10,0),1,3) AS REGISTER_NUM, B.Item AS ITEM,"
             +"B.Non_Merch_Item AS NON_MERCH_ITEM, B.Qty AS QTY, B.Unit_Retail AS UNIT_RETAIL, b.Total_Igtax_Amt AS TOTAL_IGTAX_AMT, E.Av_Cost AS AV_COST From Sa_Tran_Head A Inner Join Sa_Tran_Item B On A.Store = B.Store "
             +"AND A.TRAN_SEQ_NO = B.TRAN_SEQ_NO INNER JOIN SA_CUSTOMER C ON A.STORE = C.STORE AND A.DAY = C.DAY AND A.TRAN_SEQ_NO = C.TRAN_SEQ_NO "
             +"INNER JOIN ITEM_LOC_SOH E ON B.ITEM = E.ITEM AND B.STORE = E.LOC WHERE "
             +"TRUNC(TRAN_DATETIME) = '"+date+"' AND A.TRAN_TYPE in ('SALE','RETURN','LAYCMP') AND A.STATUS = 'P' AND C.CUST_ID IN "
             +"(select card_no1 from METRO_IT_MRC_ISO_ACCNTS@METROBIP_DBLINK  where mrc1=6)";
        
       pStatement = con.prepareStatement(query);
       rs = pStatement.executeQuery();
       
       System.out.println("Creating Report..");
       file = new File("Daily ISO Sales (as of "+date2+").csv");
       file.createNewFile();
       FileWriter writer = new FileWriter(file); 
       BufferedWriter bw = new BufferedWriter(writer);
       bw.write("STORE,BUSINESS_DATE,MY_TIME,CUST_ID,POSTAL_CODE,TRAN_SEQ_NO,TRAN_NO,ITEM_SEQ_NO,REGISTER_NUM,ITEM,NON_MERCH_ITEM,QTY,UNIT_RETAIL,TOTAL_IGTAX_AMT,AV_COST\n");
       while (rs.next()){
           bw.write(rs.getString("STORE")+",");
           bw.write(rs.getString("BUSINESS_DATE")+",");
           bw.write(rs.getString("MY_TIME")+",");
           bw.write(rs.getString("CUST_ID")+",");
           bw.write(rs.getString("POSTAL_CODE")+",");
           bw.write(rs.getString("Tran_Seq_No")+",");
           bw.write(rs.getString("Tran_No")+",");
           bw.write(rs.getString("Item_Seq_No")+",");
           bw.write(rs.getString("Register_Num")+",");
           bw.write(rs.getString("Item")+",");
           bw.write(rs.getString("Non_Merch_Item")+",");
           bw.write(rs.getString("Qty")+",");
           bw.write(rs.getString("Unit_Retail")+",");
           bw.write(rs.getString("Total_Igtax_Amt")+",");
           bw.write(rs.getString("Av_Cost")+"\n");
       }
       bw.close();
       
       
   }
   private static void SendEmail(){
       String from = "Report Mailer<report.mailer@metroretail.com.ph>";
       String host = "mymail.metrogaisano.com";
      
       Properties mailproperties = new Properties();
      
     
       mailproperties.put("mail.transport.protocol", "smtp");
       mailproperties.put("mail.host", host);
       mailproperties.put("mail.smtp.auth", "false");
       mailproperties.put("mail.smtp.port", "25");
       mailproperties.put("mail.smtp.ssl.enable", "false");
       mailproperties.put("mail.smtp.starttls.enable", "false");
       
 
       Session session = Session.getInstance(mailproperties);
      

       try{
       String[] to = {"daniel.lapinig@metroretail.com.ph"};
       String[] cc = {"lea.gonzaga@metroretail.com.ph","lloydpatrick.flores@metroretail.com.ph"};
       InternetAddress[] addressTo = new InternetAddress[to.length];
       InternetAddress[] addressCc = new InternetAddress[cc.length];
        for (int i = 0; i < to.length; i++)
            {
                addressTo[i] = new InternetAddress(to[i]);
            }
        for (int i = 0; i < cc.length; i++)
            {
                addressCc[i] = new InternetAddress(cc[i]);
            }
          // Create a default MimeMessage object.
          MimeMessage message = new MimeMessage(session);
          
          // Set From: header field of the header.
         message.setFrom(new InternetAddress(from));

         // Set To: header field of the header.
         //message.addRecipient(Message.RecipientType.TO, new InternetAddress(to2));
         message.addRecipients(Message.RecipientType.TO, addressTo);
         message.addRecipients(Message.RecipientType.CC, addressCc);

         // Set Subject: header field
         message.setSubject("Daily ISO Sales (as of "+date2+")");

         // Now set the actual message
         //message.setText("This is actual message");
        
         // Adding attachments
         BodyPart messageBodyPart = new MimeBodyPart();
         messageBodyPart.setText("This is a system-generated report.  Replies to this email account are not monitored.  If you need assistance, kindly email itsd@metroretail.com.ph");
         Multipart multipart = new MimeMultipart();
         multipart.addBodyPart(messageBodyPart);
         
          // Part two is attachment
         messageBodyPart = new MimeBodyPart();
         FileDataSource source = new FileDataSource(file);
         messageBodyPart.setDataHandler(new DataHandler(source));
         messageBodyPart.setFileName(file.getName());
         multipart.addBodyPart(messageBodyPart);

         // Send the complete message parts
         message.setContent(multipart);

         
         
         
         System.out.println("Sending Message..");
         // Send message
         Transport transport = session.getTransport("smtp");
         Transport.send(message);
         transport.close();
         System.out.println("Message sent successfully.");
      }catch (MessagingException mex) {
          System.out.print(mex);
      }
   }
   
   private static void getDate(){
        try{
            String query = "Select to_char(sysdate-1,'DD MON YYYY') as Sys_date2,to_char(sysdate-1, 'DD-MON-YY') as Sys_date from DUAL";
            
            pStatement = con.prepareStatement(query);
            rs = pStatement.executeQuery();
            while (rs.next()){
                date = rs.getString("Sys_date");
                date2 = rs.getString("Sys_date2");
            }   
            //System.out.println(date);
            //System.out.println(date2);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
