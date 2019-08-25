import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

public class Main extends ListenerAdapter {
    AudioReceiver receiver = null;
    AudioSender sender = null;
    AudioManager audioManager = null;


    public static void main(String[] args) throws LoginException {
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken(Util.getDiscKey())
                .addEventListeners(new Main());
        builder.build();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().toLowerCase().equals("!start")) {
                if (audioManager == null)
                    audioManager = event.getGuild().getAudioManager();
                receiver = new AudioReceiver();
                sender = new AudioSender(audioManager);
                if (audioManager.isConnected()) {
                    event.getChannel().sendMessage("I am already connected to a voice chat!").queue();
                    return;
                }
                if (!event.getMember().getVoiceState().inVoiceChannel()) {
                    event.getChannel().sendMessage("Please connect to a voice chat first.").queue();
                    return;
                }
                audioManager.openAudioConnection(event.getMember().getVoiceState().getChannel());
                audioManager.setReceivingHandler(receiver);
                audioManager.setSendingHandler(sender);

            } else if (event.getMessage().getContentRaw().toLowerCase().equals("!end")) {
                if (!audioManager.isConnected()) {
                    event.getChannel().sendMessage("I am not connected to a call.").queue();
                    return;
                }
                VoiceChannel vc = audioManager.getConnectedChannel();
                if (!vc.getMembers().contains(event.getMember())) {
                    event.getChannel().sendMessage("Please connect to the same channel.").queue();
                    return;
                }
                audioManager.closeAudioConnection();

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                receiver.writeFile();
                File[] files = new File("audio").listFiles();
                for (File f : files) {
                    try {
                        String baseName = f.getName().substring(0, f.getName().length() - 4);
                        System.out.println("CONVERTING " + f.getName());
                        Process p = new ProcessBuilder(
                                "ffmpeg",
                                "-f", "s16be",
                                "-ar", "48k",
                                "-ac", "2",
                                "-i", "audio" + "/" + baseName + ".txt",
                                "-f", "wav",
                                "-ar", "16k",
                                "-ac", "1",
                                "audio_out" + "/" + baseName + ".wav").start();
                        p.waitFor();
                        f.delete();
                        sendToNLPServer(new File("audio_out/" + baseName + ".wav"));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    audioManager.setReceivingHandler(null);
                    audioManager.setSendingHandler(null);
                }
            } else if (event.getMessage().getContentRaw().toLowerCase().startsWith("!test ")) {
                sendToNLPServer(new File(event.getMessage().getContentRaw().substring("!test ".length())));
            }
        } else {
            System.out.println("bot");
        }
    }

    private void sendToNLPServer(File audioFile) {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(Util.getAzureKey(), "westus");
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getPath());
        SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
        speechRecognizer.recognized.addEventListener((o, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech && !e.getResult().getText().isEmpty()) {
                System.out.print("RECOGNIZED: ");
                System.out.println(e.getResult().getText());
                String url = "https://omnical-ai.azurewebsites.net/api/analyze" +
                        "?code=dCB9FpHWVhqwFYaxpJc5xq2cuQoBU1t9O30JS2SrPZL5iN/yAYw6QQ==" +
                        "&discordID=" + URLEncoder.encode(audioFile.getName().substring(audioFile.getName().indexOf('-') + 1, audioFile.getName().length() - 4)) +
                        "&text=" + URLEncoder.encode(e.getResult().getText()) +
                        "&from=" + URLEncoder.encode(audioFile.getName().substring(0, audioFile.getName().indexOf('-')));
                String out = Util.executeGet(url);
                System.out.println("STT API RESULT: " + out);
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                System.out.println("NO MATCH");
            }
        });
        speechRecognizer.sessionStopped.addEventListener((o, e) -> {
            System.out.print("DONE: ");
            System.out.println(e);
        });
        try {
            speechRecognizer.startContinuousRecognitionAsync().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}