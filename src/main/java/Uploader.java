import org.apache.http.HttpResponse;
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

    public static void imgurUpload(JFrame parent, BufferedImage img) {
        if (img == null) {
            JOptionPane.showMessageDialog(parent, "Image is empty. Did you shuffle at least once?");
            return;
        }
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        String b64img;

        try {
            ImageIO.write(img, "png", os);
            b64img = Base64.getEncoder().encodeToString(os.toByteArray());

            Request request = Request.Post(url);
            request.setHeader("Authorization", "Client-ID " + cId);
            request.bodyForm(new BasicNameValuePair("image", b64img));
            HttpResponse httpResponse = request.execute().returnResponse();
            if (httpResponse.getEntity() != null) {
                String response = EntityUtils.toString(httpResponse.getEntity());
                Matcher m = Pattern.compile("\"link\":\"(.*?)\"").matcher(response);
                if (m.find())
                    showDialog(m.group(1).replace("\\", ""), parent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
