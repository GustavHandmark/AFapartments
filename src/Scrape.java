import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;

import javax.mail.PasswordAuthentication;
import javax.mail.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class Scrape {
	//add sender, minimum apartment size, area, recipients, password/gmail authenticator-key, 
	private static int minApartmentSize =35; //availabillity in the ranges of 20 - 100 square meters.
	private static String area = "magasinet"; //magasinet, vildanden, ulrikedal, etc.
	
	private static String sender ="example@gmail.com";
	private static String authKey = "";
	private static String recipients = "example@gmail.co"; //where info will be sent.
	
	private static String msgText = "";
	
	public static void main(String[] args) throws Exception {
		try {
			ArrayList<String> lgh = getInfo();
			lgh = checkDuplicates(lgh);
			String msg = "";
			for (String s : lgh)
				if (!lgh.isEmpty()) {
					msg = msg + "\n" + s;
				}
			setMessage(msg);
			if (!msgText.isEmpty()) {
				send();
				System.out.println("Sent");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error");
			System.exit(0);

		}
	}

	public static ArrayList<String> getInfo() throws Exception {
		ArrayList<String> lgh = new ArrayList<>();
		try {
			java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
			
			WebClient webClient = new WebClient(BrowserVersion.CHROME);
			HtmlPage page1 = (HtmlPage) webClient.getPage("https://www.afbostader.se/lediga-bostader/");
			webClient.waitForBackgroundJavaScript(2000);
			List<Object> omraden = page1.getByXPath("//div[@class='hidden-xs']/h4/span[@class='area']/text()");
			List<Object> hyra = page1.getByXPath("//h3[@class='rent']/text()");
			List<Object> typ = page1.getByXPath("//div[@class='hidden-xs']/h3/a/span[@class='short-desc']/text()");
			List<Object> sqm = page1.getByXPath("//div[@class='hidden-xs']/h3/a/span[@class='sqrmtrs']/text()");
			List<Object> movdate = page1.getByXPath("//div[@class='hidden-xs']/h6[@class='move-in-date']/text()");
			
			for (int i = 0; i < omraden.size(); i++) {
				if (omraden.get(i).toString().equalsIgnoreCase(area)
						&& !typ.get(i).toString().equalsIgnoreCase("korridorrum")
						&& Double.parseDouble(sqm.get(i).toString()) > minApartmentSize) {
					StringBuilder sb = new StringBuilder();
					sb.append(omraden.get(i).toString() + " - ");
					sb.append(typ.get(i).toString() + " - ");
					sb.append(sqm.get(i) + "kvm" + " - ");
					sb.append(hyra.get(i).toString() + " - ");
					sb.append("Inflyttningsdag: " + movdate.get(i).toString());
					lgh.add(sb.toString());

				}

			}
			webClient.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("HTML ERROR");

		}
		return lgh;

	}

	public static void send() {
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() { //for send adress
				return new PasswordAuthentication(sender, authKey); //Email and password/gmail authenticator key
			}
		});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(sender));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
			message.setSubject("New apartments from AFB");
			message.setText(msgText);
			Transport.send(message);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
	//running the program once per day (as i do) generates duplicates of the same apartment objects
	//this ensures that the information is sent only once per apartment object.
	public static ArrayList<String> checkDuplicates(ArrayList<String> lgh) {
		File resourcesDir = new File(System.getProperty("user.home")+ File.separator + "AFBscrape resources");
		try {
			FileUtils.forceMkdir(resourcesDir);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		ArrayList<String> deleteCandidates = new ArrayList<String>();
		File f = new File(System.getProperty("user.home")+File.separator+"AFBscrape resources"+File.separator + "alreadyEmailed.txt");
		if (f.exists()) {
			try {
				ArrayList<String> emailed = new ArrayList<String>(FileUtils.readLines(f,Charset.defaultCharset()));
				for (String str : lgh) {
					if (emailed.contains(str)) {
						deleteCandidates.add(str);
					}
				}
				for (String deleteCandidate : deleteCandidates) {
					lgh.remove(deleteCandidate);
				}
				FileUtils.writeLines(f, lgh,true);

			} catch (Exception e) {
				System.out.println("error");
				return lgh;
			}

		} else if (!f.exists()) {
			try {
				f.createNewFile();
				FileUtils.writeLines(f, lgh,true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return lgh;
	}


	public static String setMessage(String text) {
		msgText = text;
		return msgText;
	}

	public static String setRecipients(String recip) {
		recipients = recip;
		return msgText;
	}




}
