import net.dv8tion.jda.api.entities.User;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;

class Util {
    public static String customID(User user) {
        return user.getName() + "-" + user.getId();
    }

    public static String executeGet(final String https_url) {
        String ret = "";

        URL url;
        try {

            HttpsURLConnection con;
            url = new URL(https_url);
            con = (HttpsURLConnection) url.openConnection();
            ret = con.getContent().toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
    public static String getAzureKey() {
        try {
            return new String(Files.readAllBytes(new File("azure.key").toPath()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getDiscKey() {
        try {
            return new String(Files.readAllBytes(new File("discord.key").toPath()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}