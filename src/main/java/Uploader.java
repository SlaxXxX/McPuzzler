import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Uploader {

    private static final String url = "https://api.imgur.com/3/image";
    private static final String cId = "9e1d8b1416d134a";

    public static void imgurUpload(JFrame parent, BufferedImage img) throws IOException {
        if (img == null) {
            JOptionPane.showMessageDialog(parent, "Image is empty. Did you shuffle at least once?");
            return;
        }
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        String b64img;

        try {
            ImageIO.write(img, "png", os);
            b64img = Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "An error occured while encoding the image.");
            return;
        } finally {
            os.close();
        }

        Request request = Request.Post(url);
        request.setHeader("Authorization", "Client-ID " + cId);
        request.bodyForm(new BasicNameValuePair("image", b64img));
        HttpResponse httpResponse = request.execute().returnResponse();
        int code = httpResponse.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK && httpResponse.getEntity() != null) {
            String response = EntityUtils.toString(httpResponse.getEntity());
            Matcher m = Pattern.compile("\"link\":\"(.*?)\"").matcher(response);
            if (m.find())
                showDialog(m.group(1).replace("\\", ""), parent);
        } else {
            if (httpResponse.getEntity() != null)
                throw new IOException(String.format("Server responded with Code %s\nResponse dump: %s", code, EntityUtils.toString(httpResponse.getEntity())));
            else
                throw new IOException(String.format("Server responded with Code %s\nNo response Entity!", code));
        }

    }

    private static void showDialog(String url, JFrame parent) {
        int n = JOptionPane.showOptionDialog(parent,
                url,
                "Success!",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,     //do not use a custom Icon
                new Object[]{"Copy to Clipboard"},
                "Copy to Clipboard"); //default button title
        if (n != JOptionPane.CLOSED_OPTION) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(url), null);
        }
    }
}
